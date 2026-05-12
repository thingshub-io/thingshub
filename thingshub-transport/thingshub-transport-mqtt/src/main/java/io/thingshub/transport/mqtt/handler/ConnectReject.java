package io.thingshub.transport.mqtt.handler;

import io.netty.handler.codec.mqtt.MqttMessage;

public record ConnectReject(MqttMessage farewell, String reason, boolean goawayImmediately) {

	public ConnectReject(String reason, boolean goawayImmediately) {
		this(null, reason, goawayImmediately);
	}

	public ConnectReject(String reason) {
		this(null, reason, false);
	}

}
