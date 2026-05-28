package io.thingshub.transport.mqtt.processor;

import io.thingshub.transport.Processor;
import io.thingshub.transport.mqtt.MqttChannelContext;
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
public class PubCompProcessor implements Processor<MqttChannelContext, PubCompPacket> {

	@Override
	public void process(MqttChannelContext ctx, PubCompPacket packet) {
		if (log.isDebugEnabled()) {
			log.debug("Client send PUBCOMP packet");
		}
	}

}
