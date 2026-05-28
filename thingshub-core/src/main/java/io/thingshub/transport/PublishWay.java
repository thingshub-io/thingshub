package io.thingshub.transport;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum PublishWay {
	PUBLISH(1), LASTWILL(2);

	@Accessors(fluent = true)
	@Getter
	private final int value;

	PublishWay(int value) {
		this.value = value;
	}

	public static PublishWay of(int value) {
		return switch (value) {
		case 1 -> PUBLISH;
		case 2 -> LASTWILL;
		default -> null;
		};
	}
}