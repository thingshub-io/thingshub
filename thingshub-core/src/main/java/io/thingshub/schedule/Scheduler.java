package io.thingshub.schedule;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.thingshub.ioc.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * <p>
 * Timing Task Scheduler
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Component
public class Scheduler {

	private final Map<String, Map<String, Task>> groupTasksMap = new ConcurrentHashMap<>();

	private HashedWheelTimer wheelTimer;

	@PostConstruct
	public void init() {
		ExecutorService taskExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
		this.wheelTimer = new HashedWheelTimer(Executors.defaultThreadFactory(), 10, TimeUnit.MILLISECONDS, 512, true, -1, taskExecutor);
	}

	public void addTask(String group, String taskId, Action action, long delay, int times) {
		Task task = new Task(action, delay, times);
		Timeout timeout = this.wheelTimer.newTimeout(task, delay, TimeUnit.MILLISECONDS);
		task.setTimeout(timeout);

		Map<String, Task> taskMap = groupTasksMap.computeIfAbsent(group, grp -> new ConcurrentHashMap<>());
		taskMap.put(taskId, task);
		groupTasksMap.put(group, taskMap);
	}

	public void cancelTask(String group, String taskId) {
		Optional.ofNullable(groupTasksMap.get(group)).flatMap(taskMap -> Optional.ofNullable(taskMap.remove(taskId))).ifPresent(Task::cancel);
	}

	public void cancelTasks(String group) {
		Optional.ofNullable(groupTasksMap.get(group)).flatMap(taskMap -> Optional.ofNullable(taskMap.keySet())).ifPresent(taskIds -> {
			taskIds.forEach(taskId -> groupTasksMap.get(group).remove(taskId).cancel());
			groupTasksMap.remove(group);
		});
	}

	@PreDestroy
	public void close() {
		this.wheelTimer.stop();
	}

}