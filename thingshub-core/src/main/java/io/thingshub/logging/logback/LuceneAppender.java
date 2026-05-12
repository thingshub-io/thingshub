package io.thingshub.logging.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class LuceneAppender extends AbstractLuceneAppender<ILoggingEvent, AppenderListener<ILoggingEvent>> {

	public static String dir() {
		return INDEX_DIR;
	}

	private boolean includeCallerData;

	@Override
	protected void prepareForDeferredProcessing(final ILoggingEvent event) {
		super.prepareForDeferredProcessing(event);
		if (includeCallerData) {
			event.getCallerData();
		}
	}

	public boolean isIncludeCallerData() {
		return includeCallerData;
	}

	public void setIncludeCallerData(boolean includeCallerData) {
		this.includeCallerData = includeCallerData;
	}

}