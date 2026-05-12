package io.thingshub.commons.model;

public enum MessageType {
	REQUEST, REPLY;

	public static MessageType of(String type) {
		return switch (type.toUpperCase()) {
		case "REQUEST" -> REQUEST;
		case "REPLY" -> REPLY;
		default -> throw new IllegalArgumentException("invalid message type: " + type);
		};
	}
};