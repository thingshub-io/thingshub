package io.thingshub.logging.logback;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;

public class DelegatingStatusListener extends ContextAwareBase implements StatusListener, LifeCycle {

	private StatusListener delegate;

	private volatile boolean started;

	@Override
	public void start() {
		if (delegate == null) {
			addError("delegate must be configured");
			return;
		}
		if (delegate instanceof ContextAware listener) {
			listener.setContext(context);
		}
		if (delegate instanceof LifeCycle listener) {
			listener.start();
		}
		started = true;
	}

	@Override
	public void stop() {
		if (delegate instanceof LifeCycle listener) {
			listener.stop();
		}
		started = false;
	}

	@Override
	public boolean isStarted() {
		return started;
	}

	@Override
	public void addStatusEvent(Status status) {
		delegate.addStatusEvent(status);
	}

	@Override
	public void setContext(Context context) {
		super.setContext(context);
	}

	public StatusListener getDelegate() {
		return delegate;
	}

	public void setDelegate(StatusListener delegate) {
		this.delegate = delegate;
	}
}
