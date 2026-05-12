package io.thingshub.transport;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum ConnectState {
	OFF_LINE(0), ON_LINE(1);

	@Accessors(fluent = true)
	@Getter
	private int value;

	ConnectState(int value) {
		this.value = value;
	}

};