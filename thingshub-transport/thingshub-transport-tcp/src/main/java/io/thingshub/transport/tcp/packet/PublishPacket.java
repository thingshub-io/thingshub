package io.thingshub.transport.tcp.packet;

import io.thingshub.commons.ThingshubMessage;
import io.thingshub.transport.Packet;
import io.thingshub.transport.TransportPacket;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class PublishPacket extends TransportPacket {

	public PublishPacket(int packetId, ThingshubMessage payload) {
		this.packetId = packetId;
		this.packetType = Packet.PUBLISH.value();
		this.packetName = Packet.PUBLISH.name();
		this.timestamp = System.currentTimeMillis();
		this.payload = payload;
	}

}
