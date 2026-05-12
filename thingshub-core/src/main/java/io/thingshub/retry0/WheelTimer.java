package io.thingshub.retry0;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class WheelTimer {

	private final long duration;

	private final int slots;

	private final long interval;

	private final Queue<TaskHolder>[] buckets;

	private volatile long currentTime;

	private ExecutorService taskExecutor;

	private AtomicLong taskCounter;

	private ConcurrentMap<Long, TaskHolder> taskRegistry;

	private volatile WheelTimer overflowWheel;

	@SuppressWarnings("unchecked")
	WheelTimer(long duration, int slots, long startTime, ExecutorService taskExecutor, AtomicLong taskCounter, ConcurrentMap<Long, TaskHolder> taskRegistry) {
		this.duration = duration;
		this.slots = slots;
		this.interval = duration * slots;
		this.currentTime = startTime - (startTime % duration); // 对齐时间槽
		this.taskExecutor = taskExecutor;
		this.taskCounter = taskCounter;
		this.taskRegistry = taskRegistry;

		this.buckets = new Queue[slots];
		for (int i = 0; i < slots; i++) {
			buckets[i] = new ConcurrentLinkedQueue<>();
		}
	}

	void addTask(TaskHolder taskHolder) {
		long startAt = taskHolder.task.getStartAt();
		if (startAt < currentTime + duration) {
			taskExecutor.submit(taskHolder.task.getCommand());
			taskCounter.decrementAndGet();

			return;
		}

		if (startAt < currentTime + interval) {
			long virtualId = startAt / duration;
			int bucketIndex = (int) (virtualId % slots);

			taskHolder.ownerWheel = this;
			taskHolder.bucketIndex = bucketIndex;

			buckets[bucketIndex].add(taskHolder);

			return;
		}

		if (overflowWheel == null) {
			synchronized (this) {
				if (overflowWheel == null) {
					overflowWheel = new WheelTimer(interval, 5, currentTime, taskExecutor, taskCounter, taskRegistry);
					overflowWheel.addTask(taskHolder);
				}
			}
		}
	}

	boolean removeTask(TaskHolder holder, int bucketIndex) {
		return buckets[bucketIndex].remove(holder);
	}

	void advance(long timestamp) {
		long timeToAdvance = timestamp - currentTime;
		if (timeToAdvance < duration) {
			log.info("timeToAdvance < tickDuration===========" + (timeToAdvance < duration));
			return;
		}

		currentTime = timestamp - (timestamp % duration);
		int bucketIndex = (int) ((timestamp / duration) % slots);
		Queue<TaskHolder> bucket = buckets[bucketIndex];

		int processed = 0;
		Iterator<TaskHolder> itr = bucket.iterator();
		while (itr.hasNext()) {
			TaskHolder holder = itr.next();

			if (holder.cancelled) {
				itr.remove();
				taskRegistry.remove(holder.task.getTaskId(), holder);
				taskCounter.decrementAndGet();
				continue;
			}

			if (holder.task.getStartAt() <= currentTime) {
				itr.remove();
				executeOrSchedule(holder);
				processed++;
			}

			if (processed > 10_000)
				break;
		}

		// 推进上层时间轮
		if (overflowWheel != null) {
			overflowWheel.advance(timestamp);
		}
	}

	private void executeOrSchedule(TaskHolder holder) {
		if (taskRegistry.remove(holder.task.getTaskId(), holder)) {
			taskExecutor.submit(() -> {
				if (!holder.cancelled) {
					holder.task.getCommand().run();
				}
			});
			taskCounter.decrementAndGet();
		}
	}

}