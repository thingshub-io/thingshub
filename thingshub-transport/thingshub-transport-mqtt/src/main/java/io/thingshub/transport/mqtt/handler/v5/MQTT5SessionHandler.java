package io.thingshub.transport.mqtt.handler.v5;

import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.CONTENT_TYPE;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.CORRELATION_DATA;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.PAYLOAD_FORMAT_INDICATOR;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.PUBLICATION_EXPIRY_INTERVAL;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.REASON_STRING;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.RESPONSE_TOPIC;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.SESSION_EXPIRY_INTERVAL;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.SUBSCRIPTION_IDENTIFIER;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.TOPIC_ALIAS;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.USER_PROPERTY;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttProperties.BinaryProperty;
import io.netty.handler.codec.mqtt.MqttProperties.IntegerProperty;
import io.netty.handler.codec.mqtt.MqttProperties.StringPair;
import io.netty.handler.codec.mqtt.MqttProperties.StringProperty;
import io.netty.handler.codec.mqtt.MqttProperties.UserProperty;
import io.netty.handler.codec.mqtt.MqttPubReplyMessageVariableHeader;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttReasonCodeAndPropertiesVariableHeader;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubAckPayload;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.thingshub.config.TenantSettings;
import io.thingshub.service.model.ClientInfo;
import io.thingshub.service.model.ClientType;
import io.thingshub.service.model.Delivery;
import io.thingshub.subscribe.ClientSubscribe;
import io.thingshub.subscribe.TopicUtils;
import io.thingshub.transport.ChannelContextWrapper;
import io.thingshub.transport.mqtt.handler.MQTTSessionHandler;
import io.thingshub.transport.mqtt.handler.ValidateMessageResult;
import io.thingshub.transport.mqtt.handler.ValidateMessageResult.Action;
import io.thingshub.transport.mqtt.packet.LastWillTestament;
import io.thingshub.transport.utils.UTF8Utils;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * MQTT5 session handler
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class MQTT5SessionHandler extends MQTTSessionHandler {

	private final Map<String, Integer> topicToAlias = new HashMap<>();

	private final Map<Integer, String> aliasToTopic = new HashMap<>();

	public MQTT5SessionHandler(TenantSettings tenantSettings, ClientInfo clientInfo, int keepAlive, boolean sessionPresent, int sessionExpiryInterval, int receiveMaxium,
			LastWillTestament lwt, boolean requestProblemInfo) {
		super(tenantSettings, clientInfo, keepAlive, sessionPresent, sessionExpiryInterval, receiveMaxium, lwt, requestProblemInfo);
	}

	@Override
	protected int getProtocolVersion() {
		return 5;
	}

	@Override
	protected void handleDuplicateConnectMessage(MqttConnectMessage connectMessage) {
		log.error("Protocol violation: duplicate connect packet [MQTT5-3.1.0-2]]");

		MqttProperties props = new MqttProperties();
		props.add(new StringProperty(REASON_STRING.value(), "Duplicate connection packet"));
		MqttMessage response = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.ProtocolError.value()).properties(props).build();

		mqttChannelContextWrapper.goAwayWithFarewell(response, false);

	}

	@Override
	public ValidateMessageResult validatePublish(MqttPublishMessage publishMessage) {
		if (publishMessage.fixedHeader().isRetain() && !tenantSettings.isRetainEnabled()) {
			log.error("Protocol violation: retain not supported [MQTT5-3.2.2-14]");

			return new ValidateMessageResult(Action.FAREWELL, MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.RetainNotSupported.value()).build());
		}

		if (this.clientInfo.clientType() == ClientType.DEVICE && publishMessage.fixedHeader().qosLevel().value() > tenantSettings.getMaxMqttQoS().value()) {
			log.error("Protocol violation: QoS {} not supported [MQTT5-3.2.2-11]", publishMessage.fixedHeader().qosLevel().value());

			return new ValidateMessageResult(Action.FAREWELL, MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.QoSNotSupported.value()).build());
		}

		IntegerProperty propPublicationExpiryInterval = (IntegerProperty) publishMessage.variableHeader().properties().getProperty(PUBLICATION_EXPIRY_INTERVAL.value());
		if (propPublicationExpiryInterval != null && propPublicationExpiryInterval.value() > 0) {
			log.error("Protocol violation: MessageExpiryInterval must be positive integer");

			MqttProperties props = new MqttProperties();
			props.add(new StringProperty(REASON_STRING.value(), "MessageExpiryInterval must be positive integer"));
			MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.ProtocolError.value()).properties(props).build();

			return new ValidateMessageResult(Action.FAREWELL, farewell);
		}

		IntegerProperty propSubscriptionIdentifier = (IntegerProperty) publishMessage.variableHeader().properties().getProperty(SUBSCRIPTION_IDENTIFIER.value());
		if (propSubscriptionIdentifier != null) {
			log.error("Protocol violation: PUBLISH packet sent from a Client to a Server MUST NOT contain a Subscription Identifier [MQTT5-3.3.4-6]");

			MqttProperties props = new MqttProperties();
			props.add(new StringProperty(REASON_STRING.value(), "MQTT5-3.3.4-6"));
			MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.ProtocolError.value()).properties(props).build();

			return new ValidateMessageResult(Action.FAREWELL, farewell);
		}

		String topic = publishMessage.variableHeader().topicName();
		if (!UTF8Utils.isWellFormed(topic, false)) {
			log.error("Protocol violation: malformed topic [{}] in publish", topic);

			return new ValidateMessageResult(Action.FAREWELL, MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.MalformedPacket.value()).build());
		}

		if (publishMessage.fixedHeader().qosLevel().value() == MqttQoS.AT_MOST_ONCE.value() && publishMessage.fixedHeader().isDup()) {
			log.error("Protocol violation: DUP flag MUST be set to 0 for all QoS 0 messages [MQTT5-3.3.1-2]");

			MqttProperties props = new MqttProperties();
			props.add(new StringProperty(REASON_STRING.value(), "MQTT5-3.3.1-2"));
			MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.ProtocolError.value()).properties(props).build();

			return new ValidateMessageResult(Action.FAREWELL, farewell);
		}

		StringProperty propResponseTopic = (StringProperty) publishMessage.variableHeader().properties().getProperty(RESPONSE_TOPIC.value());
		if (propResponseTopic != null) {
			if (!UTF8Utils.isWellFormed(propResponseTopic.value(), false)) {
				log.error("Protocol violation: malformed response topic [{}] in publish [MQTT5-3.2.2-13]", propResponseTopic.value());

				return new ValidateMessageResult(Action.FAREWELL, MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.MalformedPacket.value()).build());
			}

			if (!TopicUtils.isValidTopic(propResponseTopic.value(), tenantSettings.getMaxTopicLevelLength(), tenantSettings.getMaxTopicLevels(),
					tenantSettings.getMaxTopicLength())) {
				log.error("Protocol violation: invalid response topic [{}]  and check topic level length, max topic levels or max topic length", propResponseTopic.value());

				return new ValidateMessageResult(Action.FAREWELL, MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.TopicNameInvalid.value()).build());
			}
		}

		if (tenantSettings.payloadFormatValidationEnabled) {
			IntegerProperty propPayloadFormatIndicator = (IntegerProperty) publishMessage.variableHeader().properties().getProperty(PAYLOAD_FORMAT_INDICATOR.value());
			if (propPayloadFormatIndicator != null && propPayloadFormatIndicator.value() == 1) {
				if (!UTF8Utils.isValidUTF8Payload(publishMessage.payload().nioBuffer())) {
					switch (publishMessage.fixedHeader().qosLevel()) {
					case AT_MOST_ONCE -> {
						log.error("Protocol violation: invalid payload format [MQTT5-3.3.2.3.2]");

						MqttProperties props = new MqttProperties();
						props.add(new StringProperty(REASON_STRING.value(), "MQTT5-3.3.2.3.2"));
						MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.PayloadFormatInvalid.value()).properties(props).build();

						return new ValidateMessageResult(Action.FAREWELL, farewell);
					}
					case AT_LEAST_ONCE -> {
						MqttMessage pubAck = MqttMessageBuilders.pubAck().packetId(publishMessage.variableHeader().packetId())
								.reasonCode(MQTT5PubAckReasonCode.PayloadFormatInvalid.value()).build();

						return new ValidateMessageResult(Action.RESPONSE, pubAck);
					}
					case EXACTLY_ONCE -> {
						MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 2);
						MqttMessageIdVariableHeader varHeader = new MqttPubReplyMessageVariableHeader(publishMessage.variableHeader().packetId(),
								MQTT5PubRecReasonCode.PayloadFormatInvalid.value(), new MqttProperties());
						MqttMessage pubRec = new MqttMessage(fixedHeader, varHeader);

						return new ValidateMessageResult(Action.RESPONSE, pubRec);
					}
					default -> {
						log.error("Protocol violation: invalid publish QoS {}", publishMessage.fixedHeader().qosLevel().value());

						MqttProperties invalidQosProps = new MqttProperties();
						invalidQosProps.add(new StringProperty(REASON_STRING.value(), "Invalid publish QoS"));
						MqttMessage theFarewell = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.ProtocolError.value()).properties(invalidQosProps).build();

						return new ValidateMessageResult(Action.FAREWELL, theFarewell);
					}
					}
				}
			}
		}

		IntegerProperty propTopicAlias = (IntegerProperty) publishMessage.variableHeader().properties().getProperty(TOPIC_ALIAS.value());
		if (propTopicAlias != null && tenantSettings.getMaxTopicAlias() == 0) {
			log.error("Protocol violation: invalid topic alias [MQTT5-3.2.2-18]");
			MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.TopicAliasInvalid.value()).build();

			return new ValidateMessageResult(Action.FAREWELL, farewell);
		}
		if (tenantSettings.getMaxTopicAlias() > 0 && propTopicAlias != null && propTopicAlias.value() > tenantSettings.getMaxTopicAlias()) {
			log.error("Protocol violation: invalid topic alias [MQTT5-3.2.2-17]");
			MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.TopicAliasInvalid.value()).build();

			return new ValidateMessageResult(Action.FAREWELL, farewell);
		}

		if (topic.isEmpty()) {
			if (propTopicAlias != null) {
				String aliasedTopic = aliasToTopic.get(propTopicAlias.value());
				if (aliasedTopic == null) {
					log.error("Protocol violation: [MQTT5-3.3.4]");

					MqttProperties props = new MqttProperties();
					props.add(new StringProperty(REASON_STRING.value(), "MQTT5-3.3.4"));
					MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.ProtocolError.value()).properties(props).build();

					return new ValidateMessageResult(Action.FAREWELL, farewell);
				}
			} else {
				log.error("Protocol violation: [MQTT5-3.3.4]");

				MqttProperties props = new MqttProperties();
				props.add(new StringProperty(REASON_STRING.value(), "MQTT5-3.3.4"));
				MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.ProtocolError.value()).properties(props).build();

				return new ValidateMessageResult(Action.FAREWELL, farewell);
			}
		} else {
			if (propTopicAlias != null) {
				Integer oldAlias = topicToAlias.put(topic, propTopicAlias.value());
				if (oldAlias != null) {
					aliasToTopic.remove(oldAlias);
				}
				String oldTopic = aliasToTopic.put(propTopicAlias.value(), topic);
				if (oldTopic != null) {
					topicToAlias.remove(oldTopic);
				}
			}
		}

		return new ValidateMessageResult(Action.CONTINUE, null);
	}

	@Override
	protected Integer getSessionExpiryIntervalInDisconnect(MqttMessage disconnectMessage) {
		MqttReasonCodeAndPropertiesVariableHeader variableHeader = (MqttReasonCodeAndPropertiesVariableHeader) disconnectMessage.variableHeader();
		IntegerProperty propSessionExpiryInterval = (IntegerProperty) variableHeader.properties().getProperty(SESSION_EXPIRY_INTERVAL.value());

		return propSessionExpiryInterval != null ? propSessionExpiryInterval.value() : null;
	}

	@Override
	protected boolean isDisconnectWithLwt(MqttMessage disconnectMessage) {
		MqttReasonCodeAndPropertiesVariableHeader variableHeader = (MqttReasonCodeAndPropertiesVariableHeader) disconnectMessage.variableHeader();
		return variableHeader.reasonCode() == MQTT5DisconnectReasonCode.DisconnectWithWillMessage.value();
	}

	@Override
	protected void handleReceivingMaximumExceeded(MqttPublishMessage publishMessage) {
		MqttMessage response = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.ReceiveMaximumExceeded.value()).build();
		mqttChannelContextWrapper.goAwayWithFarewell(response, false);
	}

	@Override
	protected void handlePublishRateExceeded(MqttPublishMessage publishMessage) {
		MqttMessage response = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.MessageRateToHigh.value()).build();
		mqttChannelContextWrapper.goAwayWithFarewell(response, false);
	}

	@Override
	protected String getPublishTopic(MqttPublishMessage publishMessage) {
		String topic = publishMessage.variableHeader().topicName();
		if (topic.isEmpty()) {
			IntegerProperty propTopicAlias = (IntegerProperty) publishMessage.variableHeader().properties().getProperty(TOPIC_ALIAS.value());
			if (propTopicAlias != null) {
				String aliasedTopic = aliasToTopic.get(propTopicAlias.value());
				if (aliasedTopic != null) {
					topic = aliasedTopic;
				}
			}
		}

		return topic;
	}

	@Override
	protected int getPubRecReasonCode(MqttMessage pubRecMessage) {
		MqttPubReplyMessageVariableHeader variableHeader = (MqttPubReplyMessageVariableHeader) pubRecMessage.variableHeader();
//		MQTT5PubRecReasonCode reasonCode = MQTT5PubRecReasonCode.of(variableHeader.reasonCode());
//		return reasonCode == MQTT5PubRecReasonCode.Success || reasonCode == MQTT5PubRecReasonCode.NoMatchingSubscribers;
		return variableHeader.reasonCode();
	}

	@Override
	protected ValidateMessageResult validateSubscription(MqttSubscribeMessage subscribeMessage) {
		List<MqttTopicSubscription> subscriptions = subscribeMessage.payload().topicSubscriptions();

		if (subscriptions.isEmpty()) {
			log.error("Protocol violation: topic is empty [MQTT5-3.8.3-2]");

			MqttProperties props = new MqttProperties();
			props.add(new StringProperty(REASON_STRING.value(), "MQTT5-3.8.3-2"));
			MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.ProtocolError.value()).properties(props).build();

			return new ValidateMessageResult(Action.FAREWELL, farewell);
		}

		if (subscriptions.size() > tenantSettings.getMaxTopicFiltersPerSub()) {
			log.error("Resource limit: max topics per subscription exceed");

			MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.AdministrativeAction.value()).build();

			return new ValidateMessageResult(Action.FAREWELL, farewell);
		}

		for (MqttTopicSubscription subscription : subscriptions) {
			if (!UTF8Utils.isWellFormed(subscription.topicName(), false)) {
				log.error("Protocol violation: malformed topic [{}] in subscribe", subscription.topicName());

				MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.MalformedPacket.value()).build();

				return new ValidateMessageResult(Action.FAREWELL, farewell);
			}
		}

		IntegerProperty propSubscriptionIdentifier = (IntegerProperty) subscribeMessage.idAndPropertiesVariableHeader().properties().getProperty(SUBSCRIPTION_IDENTIFIER.value());
		if (propSubscriptionIdentifier != null) {
			if (propSubscriptionIdentifier.value().intValue() == 0) {
				log.error("Protocol violation: subscription identifier must be 1 to 268,435,455 [MQTT5-3.8.2.1.2]");

				MqttProperties props = new MqttProperties();
				props.add(new StringProperty(REASON_STRING.value(), "MQTT5-3.8.2.1.2"));
				MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.ProtocolError.value()).properties(props).build();

				return new ValidateMessageResult(Action.FAREWELL, farewell);
			} else {
				if (!tenantSettings.isSubscriptionIdentifierEnabled()) {
					MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
					MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(subscribeMessage.variableHeader().messageId());
					int[] reasonCodes = subscriptions.stream().mapToInt(s -> MQTT5SubAckReasonCode.SubscriptionIdentifierNotSupported.value()).toArray();
					MqttSubAckPayload mqttSubAckPayload = new MqttSubAckPayload(reasonCodes);
					MqttMessage response = new MqttSubAckMessage(mqttFixedHeader, variableHeader, mqttSubAckPayload);

					return new ValidateMessageResult(Action.RESPONSE, response);
				}
			}
		}

		return new ValidateMessageResult(Action.CONTINUE, null);
	}

	@Override
	protected ValidateMessageResult checkSubscribePacketIdUsing(MqttSubscribeMessage subscribeMessage) {
		int packetId = subscribeMessage.variableHeader().messageId();
		if (mqttChannelContextWrapper.isPacketIdUsing(packetId)) {
			MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
			MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(packetId);
			int[] reasonCodes = subscribeMessage.payload().topicSubscriptions().stream().mapToInt(s -> MQTT5SubAckReasonCode.PacketIdentifierInUse.value()).toArray();
			MqttSubAckPayload mqttSubAckPayload = new MqttSubAckPayload(reasonCodes);
			MqttMessage response = new MqttSubAckMessage(mqttFixedHeader, variableHeader, mqttSubAckPayload);

			return new ValidateMessageResult(Action.RESPONSE, response);
		}
		return new ValidateMessageResult(Action.CONTINUE, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<ClientSubscribe> buildClientSubscribesFromPacket(MqttSubscribeMessage subscribeMessage) {
		IntegerProperty propSubscriptionIdentifier = (IntegerProperty) subscribeMessage.idAndPropertiesVariableHeader().properties().getProperty(SUBSCRIPTION_IDENTIFIER.value());
		List<UserProperty> userProperties = (List<UserProperty>) subscribeMessage.idAndPropertiesVariableHeader().properties().getProperties(USER_PROPERTY.value());

		return subscribeMessage.payload().topicSubscriptions().stream().map(sub -> {
			Map<String, Object> props = new HashMap<>();
			props.put("qos", sub.qualityOfService().value());
			props.put("isRetainAsPublished", Boolean.valueOf(sub.option().isRetainAsPublished()));
			props.put("noLocal", Boolean.valueOf(sub.option().isNoLocal()));
			props.put("retainHandling", sub.option().retainHandling().value());

			if (propSubscriptionIdentifier != null) {
				props.put(String.valueOf(SUBSCRIPTION_IDENTIFIER.value()), propSubscriptionIdentifier.value());
			}

			if (userProperties != null) {
				props.put(String.valueOf(USER_PROPERTY.value()), userProperties);
			}

			String[] groupAndTopicFilter = TopicUtils.decode(sub.topicName());
			ClientType clientType = mqttChannelContextWrapper.channel().attr(ChannelContextWrapper.ATTRIBUTE_CLIENT_TYPE).get();

			return new ClientSubscribe(mqttChannelContextWrapper.getClientId(), clientType, groupAndTopicFilter[0], groupAndTopicFilter[1], props);
		}).toList();
	}

	@Override
	protected ValidateMessageResult validateUnsubscribe(MqttUnsubscribeMessage unsubscribeMessage) {
		List<String> topics = unsubscribeMessage.payload().topics();
		if (topics.isEmpty()) {
			log.error("Protocol violation: topic is empty [MQTT5-3.10.3-2]");

			MqttProperties props = new MqttProperties();
			props.add(new StringProperty(REASON_STRING.value(), "MQTT5-3.10.3-2"));
			MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.ProtocolError.value()).properties(props).build();

			return new ValidateMessageResult(Action.FAREWELL, farewell);
		}

		if (topics.size() > tenantSettings.getMaxTopicFiltersPerSub()) {
			log.error("Resource limit: max topics per subscription exceed");

			MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.AdministrativeAction.value()).build();

			return new ValidateMessageResult(Action.FAREWELL, farewell);
		}

		for (String topic : topics) {
			if (!UTF8Utils.isWellFormed(topic, false)) {
				log.error("Protocol violation: malformed topic [{}] in unsubscribe [MQTT5-3.8.3-2]", topic);

				MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(MQTT5DisconnectReasonCode.MalformedPacket.value()).build();

				return new ValidateMessageResult(Action.FAREWELL, farewell);
			}
		}

		return new ValidateMessageResult(Action.CONTINUE, null);
	}

	@Override
	protected ValidateMessageResult checkUnsubscribePacketIdUsing(MqttUnsubscribeMessage unsubscribeMessage) {
		int packetId = unsubscribeMessage.variableHeader().messageId();
		if (mqttChannelContextWrapper.isPacketIdUsing(packetId)) {
			MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
			MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(packetId);

			List<Short> reasonCodes = unsubscribeMessage.payload().topics().stream().map(t -> MQTT5UnsubAckReasonCode.PacketIdentifierInUse.value()).toList();
			MqttUnsubAckPayload unsubAckPayload = new MqttUnsubAckPayload(reasonCodes);
			MqttMessage response = new MqttUnsubAckMessage(fixedHeader, variableHeader, unsubAckPayload);

			return new ValidateMessageResult(Action.RESPONSE, response);
		}

		return new ValidateMessageResult(Action.CONTINUE, null);
	}

	@Override
	protected MqttPublishMessage buildPublishMessageFromDelivery(int packetId, MqttQoS mqttQos, Delivery delivery) {
		String topic = delivery.topic();// raw message topic
		String payload = delivery.payload();
		byte[] payloadBytes = payload.getBytes();

		MqttProperties props = new MqttProperties();

		Integer subscriptionIdentifier = (Integer) delivery.recProps().get(String.valueOf(SUBSCRIPTION_IDENTIFIER.value()));
		if (subscriptionIdentifier != null) {
			props.add(new IntegerProperty(SUBSCRIPTION_IDENTIFIER.value(), subscriptionIdentifier));
		}
		Integer topicAlias = (Integer) delivery.sendProps().get(String.valueOf(TOPIC_ALIAS.value()));
		if (topicAlias != null && topicAlias.intValue() > 0) {
			props.add(new IntegerProperty(TOPIC_ALIAS.value(), topicAlias));
		}
		Integer payloadFormatIndicator = (Integer) delivery.sendProps().get(String.valueOf(PAYLOAD_FORMAT_INDICATOR.value()));
		if (payloadFormatIndicator != null && payloadFormatIndicator.intValue() == 1) {
			props.add(new IntegerProperty(PAYLOAD_FORMAT_INDICATOR.value(), 1));
		}
		String contentType = (String) delivery.sendProps().get(String.valueOf(CONTENT_TYPE.value()));
		if (contentType != null) {
			props.add(new StringProperty(CONTENT_TYPE.value(), contentType));
		}
		String responseTopic = (String) delivery.sendProps().get(String.valueOf(RESPONSE_TOPIC.value()));
		if (responseTopic != null) {
			props.add(new StringProperty(RESPONSE_TOPIC.value(), responseTopic));
		}
		byte[] correlationData = (byte[]) delivery.sendProps().get(String.valueOf(CORRELATION_DATA.value()));
		if (correlationData != null) {
			props.add(new BinaryProperty(CORRELATION_DATA.value(), correlationData));
		}

		@SuppressWarnings("unchecked")
		List<StringPair> userProperties = (List<StringPair>) delivery.sendProps().get(String.valueOf(USER_PROPERTY.value()));
		if (userProperties != null) {
			userProperties.forEach(sp -> props.add(new UserProperty(sp.key, sp.value)));
		}

		Integer expiryInterval = (Integer) delivery.sendProps().get(String.valueOf(PUBLICATION_EXPIRY_INTERVAL.value()));
		if (expiryInterval != null && expiryInterval.intValue() < Integer.MAX_VALUE) {
			long expiryIntervalMillis = Duration.ofSeconds(expiryInterval).toMillis();
			long diff = System.currentTimeMillis() - delivery.deliverTime().getTime();
			int leftDelayInterval = (int) Duration.ofMillis(expiryIntervalMillis - diff).getSeconds();

			props.add(new IntegerProperty(PUBLICATION_EXPIRY_INTERVAL.value(), leftDelayInterval));
		}

		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, mqttQos, false, 0);
		MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topic, packetId, props);
		MqttPublishMessage publishMessage = new MqttPublishMessage(fixedHeader, variableHeader, Unpooled.wrappedBuffer(payloadBytes));

		return publishMessage;
	}

}