package io.thingshub.transport.mqtt.packet;

import java.util.HashMap;
import java.util.Map;

import io.netty.handler.codec.mqtt.MqttMessageType;
import io.thingshub.transport.TransportPacket;
import lombok.Getter;

@Getter
public class PubRecPacket extends TransportPacket {

	public PubRecPacket(int packetId, int reasonCode) {
		this.packetId = packetId;
		this.packetType = MqttMessageType.PUBREC.value();
		this.packetName = MqttMessageType.PUBREC.name();
		this.timestamp = System.currentTimeMillis();

		Map<String, Object> properties = new HashMap<>();
		properties.put("reasonCode", Integer.valueOf(reasonCode));
		this.properties = properties;
	}

}
