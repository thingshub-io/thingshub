package io.thingshub.transport.mqtt.packet;

import io.netty.handler.codec.mqtt.MqttMessageType;
import io.thingshub.transport.TransportPacket;

public class PubRelPacket extends TransportPacket {

	public PubRelPacket(int packetId) {
		this.packetId = packetId;
		this.packetType = MqttMessageType.PUBREL.value();
		this.packetName = MqttMessageType.PUBREL.name();
		this.timestamp = System.currentTimeMillis();
	}

}
