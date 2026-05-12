package io.thingshub.transport.mqtt.packet;

import java.util.HashMap;
import java.util.Map;

import io.netty.handler.codec.mqtt.MqttMessageType;
import io.thingshub.commons.model.ThingshubMessage;
import io.thingshub.transport.TransportPacket;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = false)
public class PublishPacket extends TransportPacket {

	private String topic;

	public PublishPacket(int packetId, String topic, int qos, boolean isRetain, boolean isDup, Map<String, Object> props, ThingshubMessage payload) {
		this.packetId = packetId;
		this.packetType = MqttMessageType.PUBLISH.value();
		this.packetName = MqttMessageType.PUBLISH.name();
		this.timestamp = System.currentTimeMillis();

		this.topic = topic;

		Map<String, Object> properties = new HashMap<>();
		properties.put("qos", Integer.valueOf(qos));
		properties.put("isRetain", Boolean.valueOf(isRetain));
		properties.put("isDup", Boolean.valueOf(isDup));
		properties.putAll(props);

		this.payload = payload;
	}

}
