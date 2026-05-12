package io.thingshub.transport.mqtt.handler.v5;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum MQTT5UnsubAckReasonCode {
	Success(0x00), NoSubscriptionExisted(0x11), UnspecifiedError(0x80), ImplementationSpecificError(0x83), NotAuthorized(0x87), TopicFilterInvalid(0x8F),
	PacketIdentifierInUse(0x91);

	@Accessors(fluent = true)
	@Getter
	private final short value;

	MQTT5UnsubAckReasonCode(int value) {
		this.value = (short) value;
	}

	public static MQTT5UnsubAckReasonCode of(short value) {
		return switch (value) {
		case 0x00 -> Success;
		case 0x11 -> NoSubscriptionExisted;
		case 0x80 -> UnspecifiedError;
		case 0x83 -> ImplementationSpecificError;
		case 0x87 -> NotAuthorized;
		case 0x8F -> TopicFilterInvalid;
		case 0x91 -> PacketIdentifierInUse;
		default -> throw new IllegalArgumentException("Invalid UnsubAck ReasonCode: " + value);
		};
	}

}
