package io.thingshub.logging.logback;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.DeferredProcessingAware;

public interface AppenderListener<Event extends DeferredProcessingAware> {

	default void onAppenderStarted(Appender<Event> appender) {
	}

	default void onAppenderStopped(Appender<Event> appender) {
	}

	default void onEventIndexed(Appender<Event> appender, Event event, long durationInNanos) {
	}

	default void onEventIndexingFailed(Appender<Event> appender, Event event, Throwable t) {
	}

}
