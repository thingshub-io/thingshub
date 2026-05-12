package io.thingshub.retry0;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

//@Component
public class Retryer {

	private final ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor();

	private final AtomicLong taskCounter = new AtomicLong(0);

	private final AtomicLong taskIdGetter = new AtomicLong(0);

	private final ConcurrentMap<Long, TaskHolder> taskRegistry = new ConcurrentHashMap<>();

	private WheelTimer wheelTimer;

	private ExecutorService executor;

	@PostConstruct
	public void init() {
		this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2, new WorkerFactory());
		this.ticker.scheduleAtFixedRate(this::advance, 10, 10, TimeUnit.MILLISECONDS);
		this.wheelTimer = new WheelTimer(10, 100, System.currentTimeMillis(), this.executor, this.taskCounter, taskRegistry);
	}

	private void advance() {
		System.out.println("Pending tasks: " + this.pendingTasks());

		long currentTime = System.currentTimeMillis();
		System.out.printf("[Log] wheel timer ticked: %d%n", currentTime);
		wheelTimer.advance(currentTime);

		long now = System.nanoTime();
		Iterator<Map.Entry<Long, TaskHolder>> itr = taskRegistry.entrySet().iterator();
		int cleaned = 0;
		while (itr.hasNext() && cleaned < 1000) {
			Map.Entry<Long, TaskHolder> entry = itr.next();
			TaskHolder holder = entry.getValue();

			if (TimeUnit.NANOSECONDS.toMinutes(now - holder.creationTime) > 5) {
				if (holder.cancelled || taskRegistry.remove(entry.getKey(), holder)) {
					itr.remove();
					taskCounter.decrementAndGet();
					cleaned++;
				}
			}
		}

		if (taskCounter.get() > 0 && currentTime % 1000 == 0) {
			System.out.printf("[Monitor] Pending tasks: %d%n", taskCounter.get());
		}
	}

	public long addTask(Runnable task, long delay) {
		if (pendingTasks() > 1_00_000) {
			throw new RejectionException("WheelTimer overload");
		}

		if (delay <= 0) {
			executor.submit(task);
			return -1L;
		}

		long taskId = taskIdGetter.incrementAndGet();
		TimerTask theTask = new TimerTask(taskId, task, System.currentTimeMillis() + delay);
		TaskHolder taskHolder = new TaskHolder(theTask);

		taskRegistry.put(taskId, taskHolder);
		taskCounter.incrementAndGet();
		wheelTimer.addTask(taskHolder);

		return taskId;
	}

	public boolean cancelTask(long taskId) {
		TaskHolder holder = taskRegistry.get(taskId);
		if (holder == null) {
			return false;
		}

		holder.cancelled = true;
		if (taskRegistry.remove(taskId, holder)) {
			if (holder.ownerWheel != null) {
				holder.ownerWheel.removeTask(holder, holder.bucketIndex);
			}

			taskCounter.decrementAndGet();

			return true;
		}

		return false;
	}

	public long pendingTasks() {
		return taskCounter.get();
	}

	@PreDestroy
	public void shutdown() {
		ticker.shutdown();
		executor.shutdown();
		try {
			if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	// 使用示例
	public static void main(String[] args) {
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> System.out.println("Executing task at " + System.nanoTime()), 0, 20,
				TimeUnit.MILLISECONDS);
//		Retryer retryer = new Retryer();
//		retryer.init();
//
//		// 添加10万个重试任务（随机延迟0-5秒）
//		for (int i = 0; i < 100_000; i++) {
//			int taskId = i;
//			long delay = (long) (Math.random() * 500);
//			retryer.addTask(() -> System.out.println("Executing task: " + taskId), delay);
//		}
//
		// 运行30秒后关闭
		try {
			Thread.sleep(30_000);
		} catch (InterruptedException ignored) {
		}
//		retryer.shutdown();
	}
}