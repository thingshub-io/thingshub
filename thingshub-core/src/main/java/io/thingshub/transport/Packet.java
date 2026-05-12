package io.thingshub.transport;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum Packet {
	REGISTER(1), REGISTER_ACK(2), AUTH(3), AUTH_ACK(4), PUBLISH(5), PUB_ACK(6), HEARTBEAT(7), HEARTBEAT_ACK(8), CLOSE(9);

	@Accessors(fluent = true)
	@Getter
	private final int value;

	Packet(int value) {
		this.value = value;
	}

	public static Packet of(int value) {
		return switch (value) {
		case 1 -> REGISTER;
		case 2 -> REGISTER_ACK;
		case 3 -> AUTH;
		case 4 -> AUTH_ACK;
		case 5 -> PUBLISH;
		case 6 -> PUB_ACK;// TODO
		case 7 -> HEARTBEAT;
		case 8 -> HEARTBEAT_ACK;
		case 9 -> CLOSE;
		default -> throw new IllegalArgumentException("unknown packet type: " + value);
		};
	}

	public static Packet of(String name) {
		return switch (name) {
		case "register" -> REGISTER;
		case "register_ack" -> REGISTER_ACK;
		case "auth" -> AUTH;
		case "auth_ack" -> AUTH_ACK;
		case "publish" -> PUBLISH;
		case "pub_ack" -> PUB_ACK;// TODO
		case "heartbeat" -> HEARTBEAT;
		case "heartbeat_ack" -> HEARTBEAT_ACK;
		case "CLOSE" -> CLOSE;
		default -> throw new IllegalArgumentException("unknown packet type: " + name);
		};
	}
}