package io.thingshub.retry0;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

class WorkerFactory implements ThreadFactory {

	private final AtomicInteger workerCounter = new AtomicInteger(0);

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, "WheelTimerWorker-" + workerCounter.incrementAndGet());
		t.setDaemon(true);
		t.setPriority(Thread.NORM_PRIORITY);

		return t;
	}

}