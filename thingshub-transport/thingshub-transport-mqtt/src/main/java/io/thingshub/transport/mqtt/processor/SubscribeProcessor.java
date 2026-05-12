package io.thingshub.transport.mqtt.processor;

import static io.thingshub.subscribe.TopicUtils.isSharedSubscription;
import static io.thingshub.subscribe.TopicUtils.isTopicMatch;
import static io.thingshub.subscribe.TopicUtils.isValidTopicFilter;
import static io.thingshub.subscribe.TopicUtils.isWildcardTopicFilter;
import static io.thingshub.transport.mqtt.handler.RetainHandling.SEND_AT_SUBSCRIBE;
import static io.thingshub.transport.mqtt.handler.RetainHandling.SEND_AT_SUBSCRIBE_IF_NOT_YET_EXISTS;
import static io.thingshub.transport.throttler.ResourceType.TOTAL_PERSISTENT_SUBSCRIBE_PER_SECOND;
import static io.thingshub.transport.throttler.ResourceType.TOTAL_PERSISTENT_SUBSCRIPTIONS;
import static io.thingshub.transport.throttler.ResourceType.TOTAL_RETAIN_MATCH_BYTES_PER_SECOND;
import static io.thingshub.transport.throttler.ResourceType.TOTAL_RETAIN_MATCH_PER_SECONDS;
import static io.thingshub.transport.throttler.ResourceType.TOTAL_SHARED_SUBSCRIPTIONS;
import static java.util.Optional.ofNullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;

import com.google.common.collect.Lists;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.thingshub.acl.AclAction;
import io.thingshub.acl.AclManager;
import io.thingshub.service.InboxService;
import io.thingshub.service.base.IdGenerator;
import io.thingshub.service.model.Delivery;
import io.thingshub.subscribe.ClientSubscribe;
import io.thingshub.subscribe.SubscriptionManager;
import io.thingshub.subscribe.TopicUtils;
import io.thingshub.transport.DeliverySource;
import io.thingshub.transport.Processor;
import io.thingshub.transport.mqtt.MqttChannelContextWrapper;
import io.thingshub.transport.mqtt.handler.v5.MQTT5SubAckReasonCode;
import io.thingshub.transport.mqtt.packet.SubscribePacket;
import io.thingshub.transport.mqtt.service.Retain;
import io.thingshub.transport.mqtt.service.RetainService;
import io.thingshub.transport.throttler.ResourceThrottler;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * MQTT SUBSCRIBE processor
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class SubscribeProcessor implements Processor<MqttChannelContextWrapper, SubscribePacket> {

	enum SubResult {
		OK, EXISTS, NO_INBOX, EXCEED_LIMIT, NOT_AUTHORIZED, TOPIC_FILTER_INVALID, WILDCARD_NOT_SUPPORTED, SHARED_SUBSCRIPTION_NOT_SUPPORTED, SUBSCRIPTION_IDENTIFIER_NOT_SUPPORTED,
		REJECTED, ERROR;
	};

	@Inject
	private AclManager aclManager;

	@Inject
	private ResourceThrottler resourceThrottler;

	@Inject
	private SubscriptionManager subscriptionManager;

	@Inject
	private RetainService retainService;

	@Inject
	private InboxService inboxService;

	@Inject
	private IdGenerator idGenerator;

	@SuppressWarnings("unchecked")
	@Override
	public void process(MqttChannelContextWrapper ctx, SubscribePacket packet) {
		List<ClientSubscribe> clientSubscribes = (List<ClientSubscribe>) packet.getPayload().getParams();
		log.info("Client send SUBSCRIBE packet, topics: {}", clientSubscribes);

		ctx.addUsingPacketId(packet.getPacketId());
		List<SubResult> subResults = clientSubscribes.stream().map(sub -> {
			SubResult finalResult = null;

			try {
				SubResult checkResult = checkSubscribe(ctx, sub);
				finalResult = checkResult;
				if (checkResult == SubResult.OK) {
					if (subscriptionManager.hasSubscribed(sub.getClientId(), sub.getGroup(), sub.getTopicFilter())) {
						finalResult = SubResult.EXISTS;

						if (sub.getGroup() == null && ctx.getTenantSettings().isRetainEnabled()) {
							Integer retainHandling = (Integer) sub.getProps().get("retainHandling");
							if (retainHandling.intValue() == SEND_AT_SUBSCRIBE.value()) {
								CompletableFuture.runAsync(() -> handleRetain(ctx, sub), ctx.executor()).exceptionally(e -> {
									log.error("Handling retain failed when subscribing topic {}. Error: ", sub.getTopicFilter(), e);
									return null;
								});
							}
						}
					} else {
						subscriptionManager.subscribe(sub);

						if (sub.getGroup() == null && ctx.getTenantSettings().isRetainEnabled()) {
							Integer retainHandling = (Integer) sub.getProps().get("retainHandling");
							if (retainHandling.intValue() == SEND_AT_SUBSCRIBE.value() || //
									retainHandling.intValue() == SEND_AT_SUBSCRIBE_IF_NOT_YET_EXISTS.value()) {
								CompletableFuture.runAsync(() -> handleRetain(ctx, sub), ctx.executor()).exceptionally(e -> {
									log.error("Handling retain failed when subscribing topic filter {}. Error: ", sub.getTopicFilter(), e);
									return null;
								});
							}
						}
					}
				}
			} catch (Exception e) {
				log.error("Subscribing topic filter {} failed. Error: ", sub.getTopicFilter(), e);

				finalResult = SubResult.ERROR;
			}

			return finalResult;
		}).toList();

		MqttSubAckMessage subAckMsg;
		if (Integer.parseInt(ctx.getProtocolVersion()) < 5) {
			subAckMsg = buildV3SubAck(ctx, packet.getPacketId(), clientSubscribes, subResults);
		} else {
			subAckMsg = buildV5SubAck(ctx, packet.getPacketId(), clientSubscribes, subResults);
		}

		final Map<String, String> parentMdc = MDC.getCopyOfContextMap();
		ctx.writeAndFlush(subAckMsg).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (!future.isSuccess()) {
					ofNullable(parentMdc).ifPresent(pMdc -> MDC.setContextMap(pMdc));
					log.error("Sending SUBACK packet failed. granted QoSs: {}", subAckMsg.payload());
					if (future.cause() != null) {
						log.error("", future.cause());
					}
				}
			}

		});

		ctx.removeUsingPacketId(packet.getPacketId());
	}

	private SubResult checkSubscribe(MqttChannelContextWrapper ctx, ClientSubscribe clientSubscribe) {
		int maxTopicLevelLength = ctx.getTenantSettings().getMaxTopicLevelLength();
		int maxTopicLevels = ctx.getTenantSettings().getMaxTopicLevels();
		int maxTopicLength = ctx.getTenantSettings().getMaxTopicLength();
		if (!isValidTopicFilter(clientSubscribe.getTopicFilter(), maxTopicLevelLength, maxTopicLevels, maxTopicLength)) {
			return SubResult.TOPIC_FILTER_INVALID;
		}

		if (isWildcardTopicFilter(clientSubscribe.getTopicFilter()) && !ctx.getTenantSettings().isWildcardSubscriptionEnabled()) {
			return SubResult.TOPIC_FILTER_INVALID;
		}

		if (!aclManager.check(clientSubscribe.getClientId(), clientSubscribe.getTopicFilter(), AclAction.SUBSCRIBE)) {
			return SubResult.NOT_AUTHORIZED;
		}

		if (clientSubscribe.getGroup() != null && !resourceThrottler.hasResource(ctx.getTenantSettings().getTenant(), TOTAL_SHARED_SUBSCRIPTIONS)) {
			return SubResult.EXCEED_LIMIT;
		}

		if (!resourceThrottler.hasResource(ctx.getTenantSettings().getTenant(), TOTAL_PERSISTENT_SUBSCRIPTIONS)) {
			return SubResult.EXCEED_LIMIT;
		}
		if (!resourceThrottler.hasResource(ctx.getTenantSettings().getTenant(), TOTAL_PERSISTENT_SUBSCRIBE_PER_SECOND)) {
			return SubResult.EXCEED_LIMIT;
		}

		Integer retainHandling = (Integer) clientSubscribe.getProps().get("retainHandling");
		if (!isSharedSubscription(clientSubscribe.getTopicFilter())
				&& (retainHandling.intValue() == SEND_AT_SUBSCRIBE.value() || retainHandling.intValue() == SEND_AT_SUBSCRIBE_IF_NOT_YET_EXISTS.value())) {
			if (!resourceThrottler.hasResource(ctx.getTenantSettings().getTenant(), TOTAL_RETAIN_MATCH_PER_SECONDS)) {
				return SubResult.EXCEED_LIMIT;
			}
			if (!resourceThrottler.hasResource(ctx.getTenantSettings().getTenant(), TOTAL_RETAIN_MATCH_BYTES_PER_SECOND)) {
				return SubResult.EXCEED_LIMIT;
			}
		}

		return SubResult.OK;
	}

	private MqttSubAckMessage buildV3SubAck(MqttChannelContextWrapper ctx, int packetId, List<ClientSubscribe> clientSubscribes, List<SubResult> subResults) {
		List<MqttQoS> grantedQoSs = new ArrayList<>(subResults.size());
		for (int i = 0; i < subResults.size(); i++) {
			switch (subResults.get(i)) {
//			case REJECTED -> log.error("Too many subscriptions");
			case NOT_AUTHORIZED -> {
				log.error("Subscription not authorized");

				grantedQoSs.add(MqttQoS.FAILURE);
			}
			case ERROR -> {
				log.error("Subscription error");

				grantedQoSs.add(MqttQoS.FAILURE);
			}
			case EXCEED_LIMIT -> {
				log.error("Subscription resource limited");

				grantedQoSs.add(MqttQoS.FAILURE);
			}
			case OK, EXISTS -> {
				Integer qos = (Integer) clientSubscribes.get(i).getProps().get("qos");
				grantedQoSs.add(MqttQoS.valueOf(qos));
			}
			default -> grantedQoSs.add(MqttQoS.FAILURE);
			}
		}

		return MqttMessageBuilders.subAck().packetId(packetId).addGrantedQoses(grantedQoSs.stream().toArray(MqttQoS[]::new)).build();
	}

	private MqttSubAckMessage buildV5SubAck(MqttChannelContextWrapper ctx, int packetId, List<ClientSubscribe> clientSubscribes, List<SubResult> subResults) {
		int[] reasonCodes = new int[subResults.size()];
		assert clientSubscribes.size() == subResults.size();
		for (int i = 0; i < subResults.size(); i++) {
			switch (subResults.get(i)) {
			case OK, EXISTS -> reasonCodes[i] = (Integer) clientSubscribes.get(i).getProps().get("qos");
			case EXCEED_LIMIT -> reasonCodes[i] = MQTT5SubAckReasonCode.QuotaExceeded.value();
			case TOPIC_FILTER_INVALID -> reasonCodes[i] = MQTT5SubAckReasonCode.TopicFilterInvalid.value();
			case NOT_AUTHORIZED -> reasonCodes[i] = MQTT5SubAckReasonCode.NotAuthorized.value();
			case WILDCARD_NOT_SUPPORTED -> reasonCodes[i] = MQTT5SubAckReasonCode.WildcardSubscriptionsNotSupported.value();
			case SUBSCRIPTION_IDENTIFIER_NOT_SUPPORTED -> reasonCodes[i] = MQTT5SubAckReasonCode.SubscriptionIdentifierNotSupported.value();
			case SHARED_SUBSCRIPTION_NOT_SUPPORTED -> reasonCodes[i] = MQTT5SubAckReasonCode.SharedSubscriptionsNotSupported.value();
			default -> reasonCodes[i] = MQTT5SubAckReasonCode.UnspecifiedError.value();
			}
		}

		MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
		MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(packetId);
		MqttSubAckPayload mqttSubAckPayload = new MqttSubAckPayload(reasonCodes);

		return new MqttSubAckMessage(mqttFixedHeader, variableHeader, mqttSubAckPayload);
	}

	private void handleRetain(MqttChannelContextWrapper ctx, ClientSubscribe clientSubscribe) {
		List<String> mappedStdTopic = subscriptionManager.getMappedStdTopics(clientSubscribe.getClientId(), clientSubscribe.getTopicFilter());
		if (mappedStdTopic == null) {
			return;
		}

		mappedStdTopic.forEach(stdt -> {
			List<Retain> retains = Lists.newArrayList();

			if (stdt.indexOf(TopicUtils.SINGLE_WILDCARD) >= 0) {
				String prefixOfStdTopic = stdt.substring(0, stdt.indexOf(TopicUtils.SINGLE_WILDCARD));
				List<Retain> nearMatchedRetains = retainService.getRetainsByPrefix(prefixOfStdTopic);
				if (nearMatchedRetains != null) {
					retains.addAll(nearMatchedRetains);
				}
			} else if (stdt.endsWith(TopicUtils.MULTI_WILDCARD)) {
				String prefixOfStdTopic = stdt.substring(0, stdt.indexOf(TopicUtils.MULTI_WILDCARD));
				List<Retain> nearMatchedRetains = retainService.getRetainsByPrefix(prefixOfStdTopic);
				if (nearMatchedRetains != null) {
					retains.addAll(nearMatchedRetains);
				}
			} else {
				Retain theRetain = retainService.getRetain(stdt);
				if (theRetain != null) {
					retains.add(theRetain);
				}
			}

			retains.stream().filter(retain -> isTopicMatch(stdt, retain.getStdTopic())).forEach(retain -> {
				long expiryInterval = Duration.ofMillis(retain.getExpireTime().getTime() - System.currentTimeMillis()).toSeconds();
				long deliveryId = idGenerator.nextId();

				Delivery delivery = Delivery.builder().id(deliveryId).receiverId(clientSubscribe.getClientId()).recProps(clientSubscribe.getProps())
						.senderId(retain.getPublisherId()).sendProps(retain.getProps()).topic(retain.getTopic()).payload(retain.getPayload()).deliverySource(DeliverySource.RETAIN)
						.deliverTime(new Date()).build();

				inboxService.deliver(delivery, expiryInterval, TimeUnit.SECONDS);
			});
		});
	}

}
