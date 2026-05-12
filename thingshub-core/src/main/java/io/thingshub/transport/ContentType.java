package io.thingshub.transport;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum ContentType {
	JSON("JSON"), BINARY("二进制"), TEXT("普通文本");

	@Accessors(fluent = true)
	@Getter
	private final String title;

	ContentType(String title) {
		this.title = title;
	}

	public static ContentType of(String name) {
		return switch (name) {
		case "JSON" -> JSON;
		case "BINARY" -> BINARY;
		case "TEXT" -> TEXT;
		default -> null;
		};
	}
}