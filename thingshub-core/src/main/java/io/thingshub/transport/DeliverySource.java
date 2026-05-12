package io.thingshub.transport;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum DeliverySource {
	PUBLISH(1), LASTWILL(2), RETAIN(3);

	@Accessors(fluent = true)
	@Getter
	private final int value;

	DeliverySource(int value) {
		this.value = value;
	}

	public static DeliverySource of(int value) {
		return switch (value) {
		case 1 -> PUBLISH;
		case 2 -> LASTWILL;
		case 3 -> RETAIN;
		default -> null;
		};
	}
}