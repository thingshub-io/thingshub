package io.thingshub.transport;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum Transport {
	TCP("TCP"), UDP("UDP"), HTTP("HTTP"), MQTT("MQTT"), MQTT_WS("MQTT over Web Socket"), GB28181_TCP("GB28181 over TCP"), GB28181_UDP("GB28181 over UDP");

	@Accessors(fluent = true)
	@Getter
	private final String title;

	Transport(String title) {
		this.title = title;
	}

	public static Transport of(String name) {
		return switch (name) {
		case "TCP" -> TCP;
		case "UDP" -> UDP;
		case "HTTP" -> HTTP;
		case "MQTT" -> MQTT;
		case "MQTT_WS" -> MQTT_WS;
		case "GB28181_TCP" -> GB28181_TCP;
		case "GB28181_UDP" -> GB28181_UDP;
		default -> null;
		};
	}
}