package io.thingshub.transport.mqtt.handler;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
public enum RetainHandling {

	SEND_AT_SUBSCRIBE(0), //

	SEND_AT_SUBSCRIBE_IF_NOT_YET_EXISTS(1), //

	DONT_SEND_AT_SUBSCRIBE(2), //

	UNRECOGNIZED(-1);

	@Accessors(fluent = true)
	@Getter
	private final int value;

	private RetainHandling(int val) {
		this.value = val;
	}

	public static RetainHandling of(int value) {
		return switch (value) {
		case 0 -> SEND_AT_SUBSCRIBE;
		case 1 -> SEND_AT_SUBSCRIBE_IF_NOT_YET_EXISTS;
		case 2 -> DONT_SEND_AT_SUBSCRIBE;
		case -1 -> UNRECOGNIZED;
		default -> throw new IllegalArgumentException("Invalid RetainHandling Value: " + value);
		};
	}

}