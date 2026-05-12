package io.thingshub.transport.mqtt.handler.v5;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum MQTT5PubCompReasonCode {
	Success((byte) 0x00), PacketIdentifierNotFound((byte) 0x92);

	@Accessors(fluent = true)
	@Getter
	private final byte value;

	MQTT5PubCompReasonCode(byte value) {
		this.value = value;
	}

	public static MQTT5PubCompReasonCode of(byte value) {
		return switch (value) {
		case (byte) 0x00 -> Success;
		case (byte) 0x92 -> PacketIdentifierNotFound;
		default -> throw new IllegalArgumentException("Invalid PubComp ReasonCode: " + value);
		};
	}
}
