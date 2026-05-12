package io.thingshub.transport.mqtt.processor;

import io.thingshub.transport.Processor;
import io.thingshub.transport.mqtt.MqttChannelContextWrapper;
import io.thingshub.transport.mqtt.packet.PubCompPacket;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * MQTTT PUBCOMP processor
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class PubCompProcessor implements Processor<MqttChannelContextWrapper, PubCompPacket> {

	@Override
	public void process(MqttChannelContextWrapper ctx, PubCompPacket packet) {
		if (log.isDebugEnabled()) {
			log.debug("Client send PUBCOMP packet");
		}
	}

}
