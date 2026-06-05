package io.thingshub.transport.tcp.packet;

import cn.hutool.core.date.DateUtil;
import io.thingshub.commons.ThingshubMessage;
import io.thingshub.transport.Packet;
import io.thingshub.transport.TransportPacket;
import lombok.Getter;

public class HeartbeatPacket extends TransportPacket {

	@Getter
	private ThingshubMessage payload;

	public HeartbeatPacket(int packetId, ThingshubMessage payload) {
		this.packetId = packetId;
		this.packetType = Packet.HEARTBEAT.value();
		this.packetName = Packet.HEARTBEAT.name();
		this.timestamp = DateUtil.parse(payload.getTime()).getTime();

		this.payload = payload;
	}

}