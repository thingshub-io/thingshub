package io.thingshub.transport.mqtt.packet;

import io.netty.handler.codec.mqtt.MqttMessageType;
import io.thingshub.commons.model.ThingshubMessage;
import io.thingshub.transport.TransportPacket;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = false)
public class SubscribePacket extends TransportPacket {

	public SubscribePacket(int packetId, ThingshubMessage payload) {
		this.packetId = packetId;
		this.packetType = MqttMessageType.SUBSCRIBE.value();
		this.packetName = MqttMessageType.SUBSCRIBE.name();
		this.timestamp = System.currentTimeMillis();

		this.payload = payload;
	}
}
