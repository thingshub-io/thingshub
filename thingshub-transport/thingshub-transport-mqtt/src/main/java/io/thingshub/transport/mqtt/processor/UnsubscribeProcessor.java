package io.thingshub.transport.mqtt.processor;

import static io.thingshub.subscribe.TopicUtils.isValidTopicFilter;
import static io.thingshub.transport.throttler.ResourceType.TOTAL_PERSISTENT_UNSUBSCRIBE_PER_SECOND;
import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.MDC;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubAckPayload;
import io.thingshub.acl.AclAction;
import io.thingshub.acl.AclManager;
import io.thingshub.subscribe.ClientUnsubscribe;
import io.thingshub.subscribe.SubscriptionManager;
import io.thingshub.transport.Processor;
import io.thingshub.transport.mqtt.MqttChannelContextWrapper;
import io.thingshub.transport.mqtt.handler.v5.MQTT5UnsubAckReasonCode;
import io.thingshub.transport.mqtt.packet.UnsubscribePacket;
import io.thingshub.transport.throttler.ResourceThrottler;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * MQTT UNSUBSCRIBE processor
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class UnsubscribeProcessor implements Processor<MqttChannelContextWrapper, UnsubscribePacket> {

	@Inject
	private AclManager aclManager;

	@Inject
	private ResourceThrottler resourceThrottler;

	@Inject
	private SubscriptionManager subscriptionManager;

	@Override
	public void process(MqttChannelContextWrapper ctx, UnsubscribePacket packet) {
		List<ClientUnsubscribe> clientUnsubscribes = (List<ClientUnsubscribe>) packet.getPayload().getParams();
		log.info("Client send UNSUBSCRIBE packet, topics: {}", clientUnsubscribes);

		final Map<String, String> parentMdc = MDC.getCopyOfContextMap();
		ctx.addUsingPacketId(packet.getPacketId());
		List<CompletableFuture<UnsubResult>> unsubFutures = clientUnsubscribes.stream().map(unsub -> {
			return ctx.addFgTask(CompletableFuture.supplyAsync(() -> check(ctx, unsub), ctx.executor())//
					.thenApply(result -> {
						if (result == UnsubResult.OK) {
							boolean hasSubscribed = subscriptionManager.unsubscribe(unsub);
							if (!hasSubscribed) {
								return UnsubResult.NO_SUB;
							}
						}

						return result;
					}).exceptionally(e -> {
						ofNullable(parentMdc).ifPresent(pMdc -> MDC.setContextMap(pMdc));
						log.error("Unsubscribing topic [{}]. Error: {}", unsub.getTopicFilter(), e);

						return UnsubResult.ERROR;
					}));
		}).toList();

		CompletableFuture.allOf(unsubFutures.stream().toArray(CompletableFuture[]::new)).thenApplyAsync(v -> {
			List<UnsubResult> unsubResults = unsubFutures.stream().map(CompletableFuture::join).toList();

			ofNullable(parentMdc).ifPresent(pMdc -> MDC.setContextMap(pMdc));

			if (Integer.parseInt(ctx.getProtocolVersion()) < 5) {
				return buildV3UnsubAck(ctx, packet.getPacketId(), clientUnsubscribes, unsubResults);
			} else {
				return buildV5UnsubAck(ctx, packet.getPacketId(), clientUnsubscribes, unsubResults);
			}
		}, ctx.executor()).thenAccept(unsubAck -> {
			ctx.writeAndFlush(unsubAck).addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (!future.isSuccess()) {
						log.error("Sending UNSUBACK packet failed. Packet id: {}", packet.getPacketId());
						if (future.cause() != null) {
							log.error("", future.cause());
						}
					}
				}

			});

			ctx.removeUsingPacketId(packet.getPacketId());
		});
	}

	private UnsubResult check(MqttChannelContextWrapper ctx, ClientUnsubscribe clientUnsubscribe) {
		int maxTopicLevelLength = ctx.getTenantSettings().getMaxTopicLevelLength();
		int maxTopicLevels = ctx.getTenantSettings().getMaxTopicLevels();
		int maxTopicLength = ctx.getTenantSettings().getMaxTopicLength();
		if (!isValidTopicFilter(clientUnsubscribe.getTopicFilter(), maxTopicLevelLength, maxTopicLevels, maxTopicLength)) {
			return UnsubResult.TOPIC_FILTER_INVALID;
		}

		if (!aclManager.check(clientUnsubscribe.getClientId(), clientUnsubscribe.getTopicFilter(), AclAction.UNSUBSCRIBE)) {
			return UnsubResult.NOT_AUTHORIZED;
		}

		if (!resourceThrottler.hasResource(ctx.getTenantSettings().getTenant(), TOTAL_PERSISTENT_UNSUBSCRIBE_PER_SECOND)) {
			return UnsubResult.EXCEED_LIMIT;
		}

		return UnsubResult.OK;
	}

	@SuppressWarnings("incomplete-switch")
	private MqttUnsubAckMessage buildV3UnsubAck(MqttChannelContextWrapper ctx, int packetId, List<ClientUnsubscribe> clientUnsubscribes,
			List<UnsubResult> unsubResults) {
		for (int i = 0; i < unsubResults.size(); i++) {
			switch (unsubResults.get(i)) {
			case NOT_AUTHORIZED -> log.error("Unsubscription not authorized");
			case EXCEED_LIMIT -> log.error("Unsubscription resource limited");
			case ERROR -> log.error("Unsubscription error");
			}
		}

		return MqttMessageBuilders.unsubAck().packetId(packetId).build();
	}

	private MqttUnsubAckMessage buildV5UnsubAck(MqttChannelContextWrapper ctx, int packetId, List<ClientUnsubscribe> clientUnsubscribes,
			List<UnsubResult> unsubResults) {
		List<Short> reasonCodes = unsubResults.stream().map(unsubResult -> {
			return switch (unsubResult) {
			case OK -> MQTT5UnsubAckReasonCode.Success.value();
			case NO_SUB -> MQTT5UnsubAckReasonCode.NoSubscriptionExisted.value();
			case TOPIC_FILTER_INVALID -> MQTT5UnsubAckReasonCode.TopicFilterInvalid.value();
			case NOT_AUTHORIZED -> MQTT5UnsubAckReasonCode.NotAuthorized.value();
			default -> MQTT5UnsubAckReasonCode.UnspecifiedError.value();
			};
		}).toList();

		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
		MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(packetId);
		MqttUnsubAckPayload unsubAckPayload = new MqttUnsubAckPayload(reasonCodes);

		return new MqttUnsubAckMessage(fixedHeader, variableHeader, unsubAckPayload);
	}

	enum UnsubResult {
		OK, NO_SUB, NO_INBOX, NOT_AUTHORIZED, TOPIC_FILTER_INVALID, EXCEED_LIMIT, ERROR;
	};

}
