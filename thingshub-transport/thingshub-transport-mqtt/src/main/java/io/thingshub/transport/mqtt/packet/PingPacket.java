package io.thingshub.transport.mqtt.packet;

import io.netty.handler.codec.mqtt.MqttMessageType;
import io.thingshub.transport.TransportPacket;

public class PingPacket extends TransportPacket {

	public PingPacket() {
		this.packetId = 0;
		this.packetType = MqttMessageType.PINGREQ.value();
		this.packetName = MqttMessageType.PINGREQ.name();
		this.timestamp = System.currentTimeMillis();
	}

}
