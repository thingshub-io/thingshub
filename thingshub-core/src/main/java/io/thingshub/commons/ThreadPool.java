package io.thingshub.commons;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public abstract class ThreadPool {

	public static ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(4 * Runtime.getRuntime().availableProcessors());

}