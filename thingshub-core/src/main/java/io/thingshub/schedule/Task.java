package io.thingshub.schedule;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.Setter;

public class Task implements TimerTask {

	private final Action action;

	private final int times;

	private final long delay; // ms

	private final int multiplier = 2;

	private final AtomicInteger counter = new AtomicInteger();

	public volatile boolean cancelled = false;

	@Setter
	private Timeout timeout;

	public Task(Action action, long delay, int times) {
		this.action = action;
		this.delay = delay;
		this.times = times;
	}

	@Override
	public void run(Timeout timeout) throws Exception {
		if (!cancelled && counter.get() < times) {
			int count = counter.incrementAndGet();
			action.execute();
			timeout = timeout.timer().newTimeout(this, delay * count * multiplier, TimeUnit.MILLISECONDS);
		}
	}

	public void cancel() {
		Optional.ofNullable(timeout).ifPresent(tm -> {
			cancelled = true;

			if (!tm.isExpired()) {
				tm.cancel();
			}
		});
	}

}