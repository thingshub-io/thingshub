package io.thingshub.transport.mqtt.processor;

import io.thingshub.schedule.Scheduler;
import io.thingshub.transport.DeliveryReader;
import io.thingshub.transport.Processor;
import io.thingshub.transport.mqtt.MqttChannelContext;
import io.thingshub.transport.mqtt.packet.PubAckPacket;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * MQTT PUBACK processor
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class PubAckProcessor implements Processor<MqttChannelContext, PubAckPacket> {

	@Inject
	private DeliveryReader deliveryReader;

	@Inject
	private Scheduler scheduler;

	@Override
	public void process(MqttChannelContext ctx, PubAckPacket packet) {
		if (log.isDebugEnabled()) {
			log.debug("Client send PUBACK packet");
		}

		Long deliveryId = (Long) MqttChannelContext.getOutgoing(ctx.getChannelId().concat("_" + packet.getPacketId()).concat("_delivery"));
		if (deliveryId == null) {
			log.warn("Client send PUBACK packet while packet[id:{}] not found", packet.getPacketId());
		} else {
			deliveryReader.ackDelivery(ctx.getClientId(), deliveryId.longValue());
			scheduler.cancelTask(ctx.getChannelId(), packet.getPacketId() + "");

		}
	}

}
