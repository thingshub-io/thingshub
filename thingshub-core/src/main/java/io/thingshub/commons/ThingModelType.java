package io.thingshub.commons;

public enum ThingModelType {
	PROPERTY, SERVICE, EVENT;

	public static ThingModelType of(String type) {
		return switch (type.toUpperCase()) {
		case "PROPERTY" -> PROPERTY;
		case "SERVICE" -> SERVICE;
		case "EVENT" -> EVENT;
		default -> throw new IllegalArgumentException("invalid thing model type");
		};
	}
};