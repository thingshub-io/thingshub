package io.thingshub.transport;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum PubMethod {
	PUBLISH(1), LASTWILL(2);

	@Accessors(fluent = true)
	@Getter
	private final int value;

	PubMethod(int value) {
		this.value = value;
	}

	public static PubMethod of(int value) {
		return switch (value) {
		case 1 -> PUBLISH;
		case 2 -> LASTWILL;
		default -> null;
		};
	}
}