package io.thingshub.transport.mqtt.packet;

import java.util.HashMap;
import java.util.Map;

import io.netty.handler.codec.mqtt.MqttMessageType;
import io.thingshub.transport.TransportPacket;
import lombok.Getter;

@Getter
public class DisconnectPacket extends TransportPacket {

	public DisconnectPacket(int sessionExpiryInterval) {
		this.packetId = 0;
		this.packetType = MqttMessageType.DISCONNECT.value();
		this.packetName = MqttMessageType.DISCONNECT.name();
		this.timestamp = System.currentTimeMillis();

		Map<String, Object> props = new HashMap<>();
		props.put("sessionExpiryInterval", Integer.valueOf(sessionExpiryInterval));
		// TODO other properties

		this.properties = props;
	}

}
