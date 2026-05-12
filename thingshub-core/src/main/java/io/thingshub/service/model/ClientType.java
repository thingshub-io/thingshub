package io.thingshub.service.model;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum ClientType {

	DEVICE(1), CLIENT_USER(2);

	@Accessors(fluent = true)
	@Getter
	private final int value;

	private ClientType(int val) {
		this.value = val;
	}

	public static ClientType of(int val) {
		return switch (val) {
		case 1 -> DEVICE;
		case 2 -> CLIENT_USER;
		default -> null;
		};
	}

}