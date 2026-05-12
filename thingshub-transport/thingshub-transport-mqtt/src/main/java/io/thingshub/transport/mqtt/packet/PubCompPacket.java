package io.thingshub.transport.mqtt.packet;

import io.netty.handler.codec.mqtt.MqttMessageType;
import io.thingshub.transport.TransportPacket;

public class PubCompPacket extends TransportPacket {

	public PubCompPacket(int packetId) {
		this.packetId = packetId;
		this.packetType = MqttMessageType.PUBCOMP.value();
		this.packetName = MqttMessageType.PUBCOMP.name();
		this.timestamp = System.currentTimeMillis();
	}

}
