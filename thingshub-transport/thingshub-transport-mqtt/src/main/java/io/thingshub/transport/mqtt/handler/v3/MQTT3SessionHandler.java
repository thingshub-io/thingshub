package io.thingshub.transport.mqtt.handler.v3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.thingshub.config.TenantSettings;
import io.thingshub.service.model.ClientInfo;
import io.thingshub.service.model.ClientType;
import io.thingshub.service.model.Delivery;
import io.thingshub.subscribe.ClientSubscribe;
import io.thingshub.subscribe.TopicUtils;
import io.thingshub.transport.ChannelContextWrapper;
import io.thingshub.transport.mqtt.handler.MQTTSessionHandler;
import io.thingshub.transport.mqtt.handler.RetainHandling;
import io.thingshub.transport.mqtt.handler.ValidateMessageResult;
import io.thingshub.transport.mqtt.handler.ValidateMessageResult.Action;
import io.thingshub.transport.mqtt.packet.LastWillTestament;
import io.thingshub.transport.utils.UTF8Utils;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * MQTT3 session handler
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class MQTT3SessionHandler extends MQTTSessionHandler {

	public MQTT3SessionHandler(TenantSettings tenantSettings, ClientInfo clientInfo, int keepAlive, boolean sessionPresent, int sessionExpiryInterval, int receiveMaxium,
			LastWillTestament lwt) {
		super(tenantSettings, clientInfo, keepAlive, sessionPresent, sessionExpiryInterval, receiveMaxium, lwt, false);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		super.exceptionCaught(ctx, cause);
	}

	@Override
	protected int getProtocolVersion() {
		return 3;
	}

	@Override
	protected void handleDuplicateConnectMessage(MqttConnectMessage connectMessage) {
		log.error("protocol violation: client can't send duplicate CONNECT packet after connected"); // MQTT3-3.1.0-2

		mqttChannelContextWrapper.goAway(false);
	}

	@Override
	protected ValidateMessageResult validatePublish(MqttPublishMessage publishMessage) {
		String topic = publishMessage.variableHeader().topicName();
		int qos = publishMessage.fixedHeader().qosLevel().value();
		boolean isDup = publishMessage.fixedHeader().isDup();

		if (!UTF8Utils.isWellFormed(topic, false)) {
			log.error("Protocol violation: malformed topic [{}] in publish", topic);

			return new ValidateMessageResult(Action.JUST_CLOSE, null);
		} else if (!TopicUtils.isValidTopic(topic, tenantSettings.getMaxTopicLevelLength(), tenantSettings.getMaxTopicLevels(), tenantSettings.getMaxTopicLength())) {
			log.error("Resource limit: invalid topic [{}] and check topic level length, max topic levels or max topic length", topic);

			return new ValidateMessageResult(Action.JUST_CLOSE, null);
		} else if (qos == MqttQoS.AT_MOST_ONCE.value() && isDup) {
			log.error("Protocol violation: DUP flag MUST be set to 0 for all QoS 0 messages");

			return new ValidateMessageResult(Action.JUST_CLOSE, null);
		} else if (this.clientInfo.clientType() == ClientType.DEVICE_CLIENT && qos > tenantSettings.getMaxMqttQoS().value()) {
			log.error("Resource limit: QoS {} is disabled", qos);

			return new ValidateMessageResult(Action.JUST_CLOSE, null);
		}

		return new ValidateMessageResult(Action.CONTINUE, null);
	}

	@Override
	protected Integer getSessionExpiryIntervalInDisconnect(MqttMessage disconnectMessage) {
		return null;
	}

	@Override
	protected boolean isDisconnectWithLwt(MqttMessage disconnectMessage) {
		return false;
	}

	@Override
	protected void handleReceivingMaximumExceeded(MqttPublishMessage publishMessage) {
		// do nothing
	}

	@Override
	protected void handlePublishRateExceeded(MqttPublishMessage publishMessage) {
		// do nothing
	}

	@Override
	protected String getPublishTopic(MqttPublishMessage publishMessage) {
		return publishMessage.variableHeader().topicName();
	}

	@Override
	protected int getPubRecReasonCode(MqttMessage pubRecMessage) {
		return 0;
	}

	@Override
	protected ValidateMessageResult validateSubscription(MqttSubscribeMessage subscribeMessage) {
		List<MqttTopicSubscription> subscriptions = subscribeMessage.payload().topicSubscriptions();

		if (subscriptions.isEmpty()) {
			log.error("Protocol violation: topic is empty [MQTT3-3.8.3-3]");

			return new ValidateMessageResult(Action.JUST_CLOSE, null);
		} else if (subscriptions.size() > tenantSettings.getMaxTopicFiltersPerSub()) {
			log.error("Resource limit: too large subscription");

			return new ValidateMessageResult(Action.JUST_CLOSE, null);
		}

		return new ValidateMessageResult(Action.CONTINUE, null);
	}

	@Override
	protected ValidateMessageResult checkSubscribePacketIdUsing(MqttSubscribeMessage subscribeMessage) {
		return new ValidateMessageResult(Action.CONTINUE, null);
	}

	@Override
	protected List<ClientSubscribe> buildClientSubscribesFromPacket(MqttSubscribeMessage subscribeMessage) {
		return subscribeMessage.payload().topicSubscriptions().stream().map(sub -> {
			Map<String, Object> props = new HashMap<>();
			props.put("qos", sub.qualityOfService().value());
			props.put("isRetainAsPublished", Boolean.FALSE);
			props.put("noLocal", Boolean.FALSE);
			props.put("retainHandling", RetainHandling.SEND_AT_SUBSCRIBE.value());

			String[] groupAndTopicFilter = TopicUtils.decode(sub.topicName());
			ClientType clientType = mqttChannelContextWrapper.channel().attr(ChannelContextWrapper.ATTRIBUTE_CLIENT_TYPE).get();

			return new ClientSubscribe(mqttChannelContextWrapper.getClientId(), clientType, groupAndTopicFilter[0], groupAndTopicFilter[1], props);
		}).toList();
	}

	@Override
	protected ValidateMessageResult validateUnsubscribe(MqttUnsubscribeMessage unsubscribeMessage) {
		List<String> topics = unsubscribeMessage.payload().topics();

		if (topics.isEmpty()) {
			log.error("Protocol violation: topic is empty [MQTT3-3.10.3-2]");

			return new ValidateMessageResult(Action.JUST_CLOSE, null);
		}

		if (topics.size() > tenantSettings.getMaxTopicFiltersPerSub()) {
			log.error("Resource limit: too large unsubscription");

			return new ValidateMessageResult(Action.JUST_CLOSE, null);
		}

		for (String topicFilter : topics) {
			if (!UTF8Utils.isWellFormed(topicFilter, false)) {
				log.error("Protocol violation: malformed topic [{}] in unsubscribe", topicFilter);

			}
		}

		return new ValidateMessageResult(Action.CONTINUE, null);
	}

	@Override
	protected ValidateMessageResult checkUnsubscribePacketIdUsing(MqttUnsubscribeMessage unsubscribeMessage) {
		return new ValidateMessageResult(Action.CONTINUE, null);
	}

	@Override
	protected MqttPublishMessage buildPublishMessageFromDelivery(int packetId, MqttQoS mqttQos, Delivery delivery) {
		String topic = delivery.topic();// raw publish topic
		String payload = delivery.payload();
		byte[] payloadBytes = payload.getBytes();
		MqttPublishMessage publishMessage = MqttMessageBuilders.publish().messageId(packetId).qos(mqttQos).retained(false).topicName(topic)
				.payload(Unpooled.wrappedBuffer(payloadBytes)).build();

		return publishMessage;
	}

}