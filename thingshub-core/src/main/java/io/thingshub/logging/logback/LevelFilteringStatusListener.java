package io.thingshub.logging.logback;

import ch.qos.logback.core.status.OnPrintStreamStatusListenerBase;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;

public class LevelFilteringStatusListener extends DelegatingStatusListener {

	private int statusLevel = Status.INFO;

	@Override
	public void addStatusEvent(Status status) {
		if (status.getEffectiveLevel() >= statusLevel) {
			super.addStatusEvent(status);
		}
	}

	public void setLevel(String level) {
		if (level.trim().equalsIgnoreCase("INFO")) {
			this.statusLevel = Status.INFO;
		} else if (level.trim().equalsIgnoreCase("WARN")) {
			this.statusLevel = Status.WARN;
		} else if (level.trim().equalsIgnoreCase("ERROR")) {
			this.statusLevel = Status.ERROR;
		} else {
			throw new IllegalArgumentException(String.format("Unknown level: %s. Must be one of INFO, WARN, or ERROR.", level));
		}
	}

	public int getLevelValue() {
		return statusLevel;
	}

	public void setLevelValue(int levelValue) {
		if (levelValue < Status.INFO || levelValue > Status.ERROR) {
			throw new IllegalArgumentException(String.format("Unknown level: %d. Must be between %d and %d, inclusive", levelValue, Status.INFO, Status.ERROR));
		}
		statusLevel = levelValue;
	}

	@Override
	public void setDelegate(StatusListener delegate) {
		super.setDelegate(delegate);
		if (delegate instanceof OnPrintStreamStatusListenerBase listener) {
			listener.setRetrospective(0L);
		}
	}
}
