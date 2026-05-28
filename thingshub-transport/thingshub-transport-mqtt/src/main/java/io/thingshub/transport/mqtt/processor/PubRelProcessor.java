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
import io.thingshub.transport.Processor;
import io.thingshub.transport.mqtt.MqttChannelContext;
import io.thingshub.transport.mqtt.handler.v5.MQTT5PubCompReasonCode;
import io.thingshub.transport.mqtt.packet.PubRelPacket;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * MQTT PUBREL processor
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class PubRelProcessor implements Processor<MqttChannelContext, PubRelPacket> {

	@Override
	public void process(MqttChannelContext ctx, PubRelPacket packet) {
		if (log.isDebugEnabled()) {
			log.debug("Client send PUBREL packet");
		}

		Object thePubPacketId = MqttChannelContext.getIncoming(ctx.getChannelId().concat("_" + packet.getPacketId()).concat("_pub"));
		MqttMessage pubCompMsg = null;
		if (thePubPacketId == null) {
			log.error("Protocol violation: client send PUBREL packet while packet[id:{}] not found", packet.getPacketId());

			MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 2);
			MqttMessageIdVariableHeader varHeader = new MqttPubReplyMessageVariableHeader(packet.getPacketId(),
					MQTT5PubCompReasonCode.PacketIdentifierNotFound.value(), new MqttProperties());
			pubCompMsg = new MqttMessage(fixedHeader, varHeader);
		} else {
			ctx.decReceivingCount();
			ctx.removeUsingPacketId(packet.getPacketId());

			if (Integer.parseInt(ctx.getProtocolVersion()) < 5) {
				MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 2);
				MqttMessageIdVariableHeader varHeader = MqttMessageIdVariableHeader.from(packet.getPacketId());
				pubCompMsg = new MqttMessage(fixedHeader, varHeader);
			} else {
				MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 2);
				MqttMessageIdVariableHeader varHeader = new MqttPubReplyMessageVariableHeader(packet.getPacketId(), MQTT5PubCompReasonCode.Success.value(),
						new MqttProperties());
				pubCompMsg = new MqttMessage(fixedHeader, varHeader);
			}
		}

		ctx.writeAndFlush(pubCompMsg).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (!future.isSuccess()) {
					log.error("Sending PUBCOMP packet[id:{}] failed", packet.getPacketId());
					if (future.cause() != null) {
						log.error("", future.cause());
					}
				}
			}

		});
	}

}
