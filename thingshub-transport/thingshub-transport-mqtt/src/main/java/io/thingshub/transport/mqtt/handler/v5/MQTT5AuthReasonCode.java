package io.thingshub.transport.mqtt.handler.v5;

public enum MQTT5AuthReasonCode {
	SUCCESS((byte) 0x00), CONTINUE((byte) 0x18), REAUTH((byte) 0x19);

	private final byte value;

	MQTT5AuthReasonCode(byte value) {
		this.value = value;
	}

	public byte value() {
		return value;
	}

	public static MQTT5AuthReasonCode of(byte value) {
		return switch (value) {
		case 0x00 -> SUCCESS;
		case 0x18 -> CONTINUE;
		case 0x19 -> REAUTH;
		default -> throw new IllegalArgumentException("Invalid Auth Reason Code: " + value);
		};
	}
}
