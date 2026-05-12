package io.thingshub.transport.mqtt.handler.v3;

import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribePayload;
import io.thingshub.transport.mqtt.MqttMessageSizer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MQTT3MessageSizer implements MqttMessageSizer {

	public static final MqttMessageSizer INSTANCE = new MQTT3MessageSizer();

	public int sizeOf(MqttMessage message) {
		switch (message.fixedHeader().messageType()) {
		case CONNECT -> {
			MqttConnectMessage connMsg = ((MqttConnectMessage) message);
			int c = 10 + sizeConnPayload(connMsg);

			return 1 + MqttMessageSizer.varIntBytes(c) + c;
		}
		case PUBLISH -> {
			MqttPublishMessage pubMsg = (MqttPublishMessage) message;
			int p = sizePubVarHeader(pubMsg.variableHeader()) + pubMsg.payload().readableBytes();
			return 1 + MqttMessageSizer.varIntBytes(p) + p;
		}
		case SUBSCRIBE -> {
			MqttSubscribeMessage subMsg = (MqttSubscribeMessage) message;
			int s = 2 + sizeSubPayload(subMsg.payload());
			return 1 + MqttMessageSizer.varIntBytes(s) + s;
		}
		case SUBACK -> {
			MqttSubAckMessage subAckMsg = (MqttSubAckMessage) message;
			int sa = 2 + sizeSubAckPayload(subAckMsg.payload());
			return 1 + MqttMessageSizer.varIntBytes(sa) + sa;
		}
		case UNSUBSCRIBE -> {
			MqttUnsubscribeMessage unsubMsg = (MqttUnsubscribeMessage) message;
			int us = 2 + sizeUnsubPayload(unsubMsg.payload());
			return 1 + MqttMessageSizer.varIntBytes(us) + us;
		}
		case CONNACK, UNSUBACK, PUBACK, PUBREC, PUBREL, PUBCOMP -> {
			return 4;
		}
		case DISCONNECT, PINGREQ, PINGRESP -> {
			return 2;
		}
		default -> {
			log.error("Unknown message type for sizing: {}", message.fixedHeader().messageType());
			return 2;
		}
		}
	}

	private int sizeConnPayload(MqttConnectMessage message) {
		MqttConnectPayload payload = message.payload();
		int clientIdBytes = MqttMessageSizer.sizeUTF8EncodedString(payload.clientIdentifier());
		int usernameBytes = message.variableHeader().hasUserName() ? MqttMessageSizer.sizeUTF8EncodedString(payload.userName()) : 0;
		int passwordBytes = message.variableHeader().hasPassword() ? MqttMessageSizer.sizeBinary(payload.passwordInBytes()) : 0;
		int payloadSize = clientIdBytes + usernameBytes + passwordBytes;
		if (message.variableHeader().isWillFlag()) {
			payloadSize += MqttMessageSizer.sizeUTF8EncodedString(payload.willTopic());
			payloadSize += MqttMessageSizer.sizeBinary(payload.willMessageInBytes());
		}

		return payloadSize;
	}

	private int sizePubVarHeader(MqttPublishVariableHeader header) {
		int topicNameBytes = MqttMessageSizer.sizeUTF8EncodedString(header.topicName());
		// A PUBLISH packet MUST NOT contain a Packet Identifier if its QoS value is set
		// to 0 [MQTT5-2.2.1-2]
		int packetIdBytes = header.packetId() == 0 ? 0 : 2;
		return topicNameBytes + packetIdBytes;
	}

	private int sizeSubPayload(MqttSubscribePayload payload) {
		int totalBytes = 0;
		for (MqttTopicSubscription sub : payload.topicSubscriptions()) {
			// 1 byte for encoding sub qos
			totalBytes += 1 + MqttMessageSizer.sizeUTF8EncodedString(sub.topicName());
		}
		return totalBytes;
	}

	private int sizeUnsubPayload(MqttUnsubscribePayload payload) {
		int totalBytes = 0;
		for (String topicFilter : payload.topics()) {
			totalBytes += MqttMessageSizer.sizeUTF8EncodedString(topicFilter);
		}
		return totalBytes;
	}

	private int sizeSubAckPayload(MqttSubAckPayload payload) {
		// 1 byte for each reason code
		return payload.reasonCodes().size();
	}

}