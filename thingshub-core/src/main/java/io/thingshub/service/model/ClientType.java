package io.thingshub.service.model;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum ClientType {

	DEVICE_CLIENT(1), SERVICE_CLIENT(2);

	@Accessors(fluent = true)
	@Getter
	private final int value;

	private ClientType(int val) {
		this.value = val;
	}

	public static ClientType of(int val) {
		return switch (val) {
		case 1 -> DEVICE_CLIENT;
		case 2 -> SERVICE_CLIENT;
		default -> null;
		};
	}

}