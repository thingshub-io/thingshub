package io.thingshub.transport.mqtt.handler.v5;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum MQTT5SubAckReasonCode {
	GrantedQoS0(0), GrantedQoS1(1), GrantedQoS2(2), UnspecifiedError(0x80), ImplementationSpecificError(0x83), NotAuthorized(0x87), TopicFilterInvalid(0x8F),
	PacketIdentifierInUse(0x91), QuotaExceeded(0x97), SharedSubscriptionsNotSupported(0x9E), SubscriptionIdentifierNotSupported(0xA1),
	WildcardSubscriptionsNotSupported(0xA2);

	@Accessors(fluent = true)
	@Getter
	private final int value;

	MQTT5SubAckReasonCode(int value) {
		this.value = value;
	}

	public static MQTT5SubAckReasonCode of(int value) {
		return switch (value) {
		case 0 -> GrantedQoS0;
		case 1 -> GrantedQoS1;
		case 2 -> GrantedQoS2;
		case 0x80 -> UnspecifiedError;
		case 0x83 -> ImplementationSpecificError;
		case 0x87 -> NotAuthorized;
		case 0x8F -> TopicFilterInvalid;
		case 0x91 -> PacketIdentifierInUse;
		case 0x97 -> QuotaExceeded;
		case 0x9E -> SharedSubscriptionsNotSupported;
		case 0xA1 -> SubscriptionIdentifierNotSupported;
		case 0xA2 -> WildcardSubscriptionsNotSupported;
		default -> throw new IllegalArgumentException("Invalid SubAck ReasonCode: " + value);
		};
	}

}
