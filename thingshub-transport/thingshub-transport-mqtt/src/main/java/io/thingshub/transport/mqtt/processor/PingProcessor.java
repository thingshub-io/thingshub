package io.thingshub.transport.mqtt.processor;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.thingshub.transport.Processor;
import io.thingshub.transport.mqtt.MqttChannelContext;
import io.thingshub.transport.mqtt.packet.PingPacket;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * MQTT PINGREQ processor
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class PingProcessor implements Processor<MqttChannelContext, PingPacket> {

	@Override
	public void process(MqttChannelContext ctx, PingPacket packet) {
		if (log.isDebugEnabled()) {
			log.debug("Client send PINGREQ packet");
		}

		ctx.writeAndFlush(MqttMessage.PINGRESP);
	}

}
