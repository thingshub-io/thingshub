package io.thingshub.transport;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum DelvieryStatus {
	DELIVERED(0), ACKED(1);

	@Accessors(fluent = true)
	@Getter
	private final int value;

	DelvieryStatus(int value) {
		this.value = value;
	}

	public static DelvieryStatus of(int value) {
		return switch (value) {
		case 0 -> DELIVERED;
		case 1 -> ACKED;
		default -> null;
		};
	}
}