package io.thingshub.commons;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum MessageType {
	PROPERTY_POST("属性上报"), //
	PROPERTY_REPLY("属性上报回复"), //
	SERVICE_FUNCTION_CALL("功能调用"), //
	SERVICE_FUNCTION_REPLY("功能调用响应"), //
	SERVICE_REQUEST_POST("提交请求"), //
	SERVICE_REQUEST_REPLY("提交请求响应"), //
	EVENT_POST("事件上报"), //
	EVENT_REPLY("事件上报回复");

	@Accessors(fluent = true)
	@Getter
	private final String title;

	MessageType(String title) {
		this.title = title;
	}

	public static MessageType of(String messageType) {
		return switch (messageType.toUpperCase()) {
		case "PROPERTY_POST" -> PROPERTY_POST;
		case "PROPERTY_REPLY" -> PROPERTY_REPLY;
		case "SERVICE_FUNCTION_CALL" -> SERVICE_FUNCTION_CALL;
		case "SERVICE_FUNCTION_REPLY" -> SERVICE_FUNCTION_REPLY;
		case "SERVICE_REQUEST_POST" -> SERVICE_REQUEST_POST;
		case "SERVICE_REQUEST_REPLY" -> SERVICE_REQUEST_REPLY;
		case "EVENT_POST" -> EVENT_POST;
		case "EVENT_REPLY" -> EVENT_REPLY;
		default -> throw new IllegalArgumentException("invalid message type: " + messageType);
		};
	}
};