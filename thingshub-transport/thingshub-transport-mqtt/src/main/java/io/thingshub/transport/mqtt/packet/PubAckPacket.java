package io.thingshub.transport.mqtt.packet;

import io.netty.handler.codec.mqtt.MqttMessageType;
import io.thingshub.transport.TransportPacket;

public class PubAckPacket extends TransportPacket {

	public PubAckPacket(int packetId) {
		this.packetId = packetId;
		this.packetType = MqttMessageType.PUBACK.value();
		this.packetName = MqttMessageType.PUBACK.name();
		this.timestamp = System.currentTimeMillis();
	}

}
