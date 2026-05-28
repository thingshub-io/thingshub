package io.thingshub.transport.mqtt.processor;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttPubReplyMessageVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.thingshub.schedule.Scheduler;
import io.thingshub.transport.DeliveryReader;
import io.thingshub.transport.Processor;
import io.thingshub.transport.mqtt.MqttChannelContext;
import io.thingshub.transport.mqtt.handler.v5.MQTT5PubRecReasonCode;
import io.thingshub.transport.mqtt.handler.v5.MQTT5PubRelReasonCode;
import io.thingshub.transport.mqtt.packet.PubRecPacket;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * MQTT PUBREC processor
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class PubRecProcessor implements Processor<MqttChannelContext, PubRecPacket> {

	@Inject
	private DeliveryReader deliveryReader;

	@Inject
	private Scheduler scheduler;

	@Override
	public void process(MqttChannelContext ctx, PubRecPacket packet) {
		if (log.isDebugEnabled()) {
			log.debug("Client send PUBREC packet");
		}

		Long deliveryId = (Long) MqttChannelContext.getOutgoing(ctx.getChannelId().concat("_" + packet.getPacketId()).concat("_delivery"));
		MqttMessage pubRelMsg = null;
		if (deliveryId == null) {
			log.error("Protocol violation: client send PUBREC packet while packet[id:{}] not found [MQTT3-4.3.3-1, MQTT5-4.3.3-8]", packet.getPacketId());
			if (Integer.parseInt(ctx.getProtocolVersion()) < 5) {
				ctx.goAway(false);
			} else {
				MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 2);
				MqttMessageIdVariableHeader varHeader = new MqttPubReplyMessageVariableHeader(packet.getPacketId(),
						MQTT5PubRelReasonCode.PacketIdentifierNotFound.value(), new MqttProperties());

				pubRelMsg = new MqttMessage(fixedHeader, varHeader);
			}
		} else {
			deliveryReader.ackDelivery(ctx.getClientId(), deliveryId.longValue());
			scheduler.cancelTask(ctx.channel().id().asLongText(), packet.getPacketId() + "");

			Integer reasonCodeValue = (Integer) packet.getProperties().get("reasonCode");
			MQTT5PubRecReasonCode reasonCode = MQTT5PubRecReasonCode.of(reasonCodeValue.byteValue());
			if (reasonCode == MQTT5PubRecReasonCode.Success || reasonCode == MQTT5PubRecReasonCode.NoMatchingSubscribers) {
				if (Integer.parseInt(ctx.getProtocolVersion()) < 5) {
					MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 2);
					MqttMessageIdVariableHeader varHeader = MqttMessageIdVariableHeader.from(packet.getPacketId());
					pubRelMsg = new MqttMessage(fixedHeader, varHeader);
				} else {
					MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 2);
					MqttMessageIdVariableHeader varHeader = new MqttPubReplyMessageVariableHeader(packet.getPacketId(), MQTT5PubRelReasonCode.Success.value(),
							new MqttProperties());
					pubRelMsg = new MqttMessage(fixedHeader, varHeader);
				}
			}
		}

		if (pubRelMsg != null) {
			ctx.writeAndFlush(pubRelMsg).addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (!future.isSuccess()) {
						log.error("Sending PUBREL packet failed");
						if (future.cause() != null) {
							log.error("", future.cause());
						}
					}
				}

			});
		}
	}

}
