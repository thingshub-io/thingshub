package io.thingshub.transport.mqtt.handler.v5;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum MQTT5PubAckReasonCode {
	Success((byte) 0x00), NoMatchingSubscribers((byte) 0x10), UnspecifiedError((byte) 0x80), ImplementationSpecificError((byte) 0x83),
	NotAuthorized((byte) 0x87), TopicNameInvalid((byte) 0x90), PacketIdentifierInUse((byte) 0x91), QuotaExceeded((byte) 0x97),
	PayloadFormatInvalid((byte) 0x99);

	@Accessors(fluent = true)
	@Getter
	private final byte value;

	MQTT5PubAckReasonCode(byte value) {
		this.value = value;
	}

	public static MQTT5PubAckReasonCode of(byte value) {
		return switch (value) {
		case (byte) 0x00 -> Success;
		case (byte) 0x10 -> NoMatchingSubscribers;
		case (byte) 0x80 -> UnspecifiedError;
		case (byte) 0x83 -> ImplementationSpecificError;
		case (byte) 0x87 -> NotAuthorized;
		case (byte) 0x90 -> TopicNameInvalid;
		case (byte) 0x91 -> PacketIdentifierInUse;
		case (byte) 0x97 -> QuotaExceeded;
		case (byte) 0x99 -> PayloadFormatInvalid;
		default -> throw new IllegalArgumentException("Invalid PubAck ReasonCode: " + value);
		};
	}
}
