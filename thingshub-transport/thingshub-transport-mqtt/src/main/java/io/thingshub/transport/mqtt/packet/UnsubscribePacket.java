package io.thingshub.transport.mqtt.packet;

import io.netty.handler.codec.mqtt.MqttMessageType;
import io.thingshub.commons.ThingshubMessage;
import io.thingshub.transport.TransportPacket;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class UnsubscribePacket extends TransportPacket {

	public UnsubscribePacket(int packetId, ThingshubMessage payload) {
		this.packetId = packetId;
		this.packetType = MqttMessageType.UNSUBSCRIBE.value();
		this.packetName = MqttMessageType.UNSUBSCRIBE.name();
		this.timestamp = System.currentTimeMillis();

		this.payload = payload;
	}
}
