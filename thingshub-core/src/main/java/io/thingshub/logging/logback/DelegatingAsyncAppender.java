package io.thingshub.logging.logback;

import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import com.lmax.disruptor.EventHandler;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import ch.qos.logback.core.spi.DeferredProcessingAware;

public abstract class DelegatingAsyncAppender<Event extends DeferredProcessingAware, Listener extends AppenderListener<Event>>
		extends AsyncAppender<Event, Listener> implements AppenderAttachable<Event> {

	private final AppenderAttachableImpl<Event> appenders = new AppenderAttachableImpl<>();

	private class DelegatingEventHandler implements EventHandler<LogEvent<Event>> {

		private boolean silentError;

		@Override
		public void onEvent(LogEvent<Event> logEvent, long sequence, boolean endOfBatch) throws Exception {

			boolean exceptionThrown = false;
			for (Iterator<Appender<Event>> it = appenders.iteratorForAppenders(); it.hasNext();) {
				Appender<Event> appender = it.next();

				try {
					appender.doAppend(logEvent.event);
					if (endOfBatch) {
						flushAppender(appender);
					}
				} catch (Exception e) {
					exceptionThrown = true;
					if (!this.silentError) {
						addError(String.format("Unable to forward event to appender [%s]: %s", appender.getName(), e.getMessage()), e);
					}
				}
			}

			this.silentError = exceptionThrown;
		}

		private void flushAppender(Appender<Event> appender) throws IOException {
			if (!appender.isStarted()) {
				return;
			}
			if (appender instanceof Flushable flushableAppender) {
				flushAppender(flushableAppender);
			} else if (appender instanceof OutputStreamAppender<Event> outAppender) {
				flushAppender(outAppender);
			}
		}

		private void flushAppender(OutputStreamAppender<Event> appender) throws IOException {
			if (!appender.isImmediateFlush()) {
				OutputStream os = appender.getOutputStream();
				if (os != null) {
					os.flush();
				}
			}
		}

		private void flushAppender(Flushable appender) throws IOException {
			appender.flush();
		}
	}

	@Override
	protected EventHandler<LogEvent<Event>> getLogEventHandler() {
		return new DelegatingEventHandler();
	}

	@Override
	public void start() {
		startDelegateAppenders();
		super.start();
	}

	@Override
	public void stop() {
		if (!isStarted()) {
			return;
		}
		super.stop();
		stopDelegateAppenders();
	}

	private void startDelegateAppenders() {
		for (Iterator<Appender<Event>> appenderIter = appenders.iteratorForAppenders(); appenderIter.hasNext();) {
			Appender<Event> appender = appenderIter.next();
			if (appender.getContext() == null) {
				appender.setContext(getContext());
			}
			if (!appender.isStarted()) {
				appender.start();
			}
		}
	}

	private void stopDelegateAppenders() {
		for (Iterator<Appender<Event>> appenderIter = appenders.iteratorForAppenders(); appenderIter.hasNext();) {
			Appender<Event> appender = appenderIter.next();
			if (appender.isStarted()) {
				appender.stop();
			}
		}
	}

	@Override
	public void addAppender(Appender<Event> newAppender) {
		appenders.addAppender(newAppender);
	}

	@Override
	public Iterator<Appender<Event>> iteratorForAppenders() {
		return appenders.iteratorForAppenders();
	}

	@Override
	public Appender<Event> getAppender(String name) {
		return appenders.getAppender(name);
	}

	@Override
	public boolean isAttached(Appender<Event> appender) {
		return appenders.isAttached(appender);
	}

	@Override
	public void detachAndStopAllAppenders() {
		appenders.detachAndStopAllAppenders();
	}

	@Override
	public boolean detachAppender(Appender<Event> appender) {
		return appenders.detachAppender(appender);
	}

	@Override
	public boolean detachAppender(String name) {
		return appenders.detachAppender(name);
	}

}