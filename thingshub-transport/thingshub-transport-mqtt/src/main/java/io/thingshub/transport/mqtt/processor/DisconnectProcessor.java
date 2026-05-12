package io.thingshub.transport.mqtt.processor;

import io.thingshub.transport.Processor;
import io.thingshub.transport.mqtt.MqttChannelContextWrapper;
import io.thingshub.transport.mqtt.packet.DisconnectPacket;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * MQTT DISCONNECT processor
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class DisconnectProcessor implements Processor<MqttChannelContextWrapper, DisconnectPacket> {

	@Override
	public void process(MqttChannelContextWrapper ctx, DisconnectPacket packet) {
		log.info("Client send DISCONNECT packet");

		ctx.goAway(true);
	}

}
