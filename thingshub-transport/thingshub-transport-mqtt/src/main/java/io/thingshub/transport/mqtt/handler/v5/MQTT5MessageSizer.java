package io.thingshub.transport.mqtt.handler.v5;

import static io.netty.buffer.ByteBufUtil.utf8Bytes;

import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdAndPropertiesVariableHeader;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttPubReplyMessageVariableHeader;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttReasonCodeAndPropertiesVariableHeader;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubAckPayload;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribePayload;
import io.thingshub.transport.mqtt.MqttMessageSizer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MQTT5MessageSizer implements MqttMessageSizer {

	public static final MqttMessageSizer INSTANCE = new MQTT5MessageSizer();

	@AllArgsConstructor
	static class MqttPropertiesBytes {

		private int minBytes;

		private int reasonStringBytes;

		private int userPropsBytes;

	}

	public int sizeOf(MqttMessage message) {
		return switch (message.fixedHeader().messageType()) {
		case CONNECT -> sizeConnVarHeader(((MqttConnectMessage) message).variableHeader()) + sizeConnPayload((MqttConnectMessage) message);
		case CONNACK -> sizeConnAckVarHeader(((MqttConnAckMessage) message).variableHeader());
		case PUBLISH -> sizePubVarHeader(((MqttPublishMessage) message).variableHeader()) + ((MqttPublishMessage) message).payload().readableBytes();
		case PUBACK -> {
			if (message.variableHeader() instanceof MqttPubReplyMessageVariableHeader pubReplyVarHeader) {
				if (pubReplyVarHeader.reasonCode() != MqttPubReplyMessageVariableHeader.REASON_CODE_OK || !pubReplyVarHeader.properties().isEmpty()) {
					yield sizePubReplyHeader(pubReplyVarHeader);
				}
			}
			// The Reason Code and Property Length can be omitted if the Reason Code is 0x00
			// (Success) and there are no Properties. In this case the PUBACK has
			// a Remaining Length of 2. [MQTT5-3.4.2.1]
			yield 4;
		}
		case PUBREC -> {
			if (message.variableHeader() instanceof MqttPubReplyMessageVariableHeader pubReplyVarHeader) {
				if (pubReplyVarHeader.reasonCode() != MqttPubReplyMessageVariableHeader.REASON_CODE_OK || !pubReplyVarHeader.properties().isEmpty()) {
					yield sizePubReplyHeader(pubReplyVarHeader);
				}
			}
			// The Reason Code and Property Length can be omitted if the Reason Code is 0x00
			// (Success) and there are no Properties. In this case the PUBREC has
			// a Remaining Length of 2 [MQTT5-3.5.2.1]

			yield 2;
		}
		case PUBREL -> {
			if (message.variableHeader() instanceof MqttPubReplyMessageVariableHeader pubReplyVarHeader) {
				if (pubReplyVarHeader.reasonCode() != MqttPubReplyMessageVariableHeader.REASON_CODE_OK || !pubReplyVarHeader.properties().isEmpty()) {
					yield sizePubReplyHeader(pubReplyVarHeader);
				}
			}
			// The Reason Code and Property Length can be omitted if the Reason Code is 0x00
			// (Success) and there are no Properties. In this case the PUBREL has
			// a Remaining Length of 2 [MQTT5-3.6.2.1]

			yield 4;
		}
		case PUBCOMP -> {
			if (message.variableHeader() instanceof MqttPubReplyMessageVariableHeader pubReplyVarHeader) {
				if (pubReplyVarHeader.reasonCode() != MqttPubReplyMessageVariableHeader.REASON_CODE_OK || !pubReplyVarHeader.properties().isEmpty()) {
					yield sizePubReplyHeader(pubReplyVarHeader);
				}
			}
			// The Reason Code and Property Length can be omitted if the Reason Code is 0x00
			// (Success) and there are no Properties. In this case the PUBCOMP has
			// a Remaining Length of 2 [MQTT5-3.7.2.1]

			yield 2;
		}
		case SUBSCRIBE -> {
			MqttSubscribeMessage subMsg = (MqttSubscribeMessage) message;
			yield sizeIdAndPropsVarHeader(subMsg.idAndPropertiesVariableHeader()) + sizeSubPayload(subMsg.payload());
		}
		case SUBACK -> {
			MqttSubAckMessage subAckMsg = (MqttSubAckMessage) message;
			yield sizeIdAndPropsVarHeader(subAckMsg.idAndPropertiesVariableHeader()) + sizeSubAckPayload(subAckMsg.payload());
		}
		case UNSUBSCRIBE -> {
			MqttUnsubscribeMessage unsubMsg = (MqttUnsubscribeMessage) message;
			yield sizeIdAndPropsVarHeader(unsubMsg.idAndPropertiesVariableHeader()) + sizeUnsubPayload(unsubMsg.payload());
		}
		case UNSUBACK -> {
			MqttUnsubAckMessage unsubAckMsg = (MqttUnsubAckMessage) message;
			yield sizeIdAndPropsVarHeader(unsubAckMsg.idAndPropertiesVariableHeader()) + sizeUnsubAckPayload(unsubAckMsg.payload());
		}
		case DISCONNECT -> {
			if (message.variableHeader() == null) {
				// The Reason Code and Property Length can be omitted if the Reason Code is 0x00
				// (Normal disconnecton) and there are no Properties. In this case
				// the DISCONNECT has a Remaining Length of 0 [MQTT5-3.14.2.1]

				yield 2;
			} else {
				yield sizeReasonCodeAndPropertiesVarHeader((MqttReasonCodeAndPropertiesVariableHeader) message.variableHeader());
			}
		}
		case PINGREQ, PINGRESP -> 2;
		case AUTH -> {
			if (message.variableHeader() == null) {
				// The Reason Code and Property Length can be omitted if the Reason Code is 0x00
				// (Success) and there are no Properties. In this case the AUTH
				// has a Remaining Length of 0 [MQTT5-3.15.2.1]

				yield 2;
			} else {
				yield sizeReasonCodeAndPropertiesVarHeader((MqttReasonCodeAndPropertiesVariableHeader) message.variableHeader());
			}
		}
		default -> {
			log.error("Unknown message type for sizing: {}", message.fixedHeader().messageType());
			yield 2;
		}
		};
	}

	private int sizeConnVarHeader(MqttConnectVariableHeader header) {
		MqttPropertiesBytes propsBytes = sizeMqttProperties(header.properties());
		// 6 bytes for UTF8 encoded string(MQTT)
		// 1 byte for ProtocolVersion
		// 1 byte for ConnectFlags
		// 2 bytes for keepAlive value
		return 10 + propsBytes.minBytes + propsBytes.reasonStringBytes + propsBytes.userPropsBytes;
	}

	private int sizeConnAckVarHeader(MqttConnAckVariableHeader header) {
		MqttPropertiesBytes propsBytes = sizeMqttProperties(header.properties());
		// 1 byte for ConnectAcknowledgeFlags
		// 1 byte for ReasonCode
		return 2 + propsBytes.minBytes + propsBytes.reasonStringBytes + propsBytes.userPropsBytes;
	}

	private int sizeReasonCodeAndPropertiesVarHeader(MqttReasonCodeAndPropertiesVariableHeader header) {
		MqttPropertiesBytes mqttPropsBytes = sizeMqttProperties(header.properties());
		// 1 byte for encoding reason code
		return 1 + mqttPropsBytes.minBytes + mqttPropsBytes.reasonStringBytes + mqttPropsBytes.userPropsBytes;
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
			MqttPropertiesBytes willPropBytes = sizeMqttProperties(payload.willProperties());
			payloadSize += willPropBytes.minBytes + willPropBytes.reasonStringBytes + willPropBytes.userPropsBytes;
		}
		return payloadSize;
	}

	private int sizePubVarHeader(MqttPublishVariableHeader header) {
		int topicNameBytes = MqttMessageSizer.sizeUTF8EncodedString(header.topicName());
		// A PUBLISH packet MUST NOT contain a Packet Identifier if its QoS value is set
		// to 0 [MQTT5-2.2.1-2]
		int packetIdBytes = header.packetId() == 0 ? 0 : 2;
		MqttPropertiesBytes propBytes = sizeMqttProperties(header.properties());
		return topicNameBytes + packetIdBytes + propBytes.minBytes + propBytes.reasonStringBytes + propBytes.userPropsBytes;
	}

	private int sizePubReplyHeader(MqttPubReplyMessageVariableHeader header) {
		MqttPropertiesBytes propBytes = sizeMqttProperties(header.properties());
		// 2 bytes for encoding packetId
		// 1 byte for encoding reason code
		return 3 + propBytes.minBytes + propBytes.reasonStringBytes + propBytes.userPropsBytes;
	}

	private int sizeIdAndPropsVarHeader(MqttMessageIdAndPropertiesVariableHeader header) {
		MqttPropertiesBytes propsBytes = sizeMqttProperties(header.properties());
		// 2 bytes for encoding packetId
		return 2 + propsBytes.minBytes + propsBytes.reasonStringBytes + propsBytes.userPropsBytes;
	}

	private int sizeSubPayload(MqttSubscribePayload payload) {
		int totalBytes = 0;
		for (MqttTopicSubscription sub : payload.topicSubscriptions()) {
			// 1 byte for encoding subscription options
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

	private int sizeUnsubAckPayload(MqttUnsubAckPayload payload) {
		// 1 byte for each reason code
		return payload.unsubscribeReasonCodes().size();
	}

	private MqttPropertiesBytes sizeMqttProperties(MqttProperties mqttProps) {
		int minBytes = 0;
		int reasonStringBytes = 0;
		int userPropsBytes = 0;
		for (MqttProperties.MqttProperty<?> mqttProperty : mqttProps.listAll()) {
			switch (MqttProperties.MqttPropertyType.valueOf(mqttProperty.propertyId())) {
			case PAYLOAD_FORMAT_INDICATOR:
				minBytes += sizePacketFormatIndicator(mqttProperty);
				break;
			case PUBLICATION_EXPIRY_INTERVAL:
				minBytes += sizeMessageExpiryInterval(mqttProperty);
				break;
			case CONTENT_TYPE:
				minBytes += sizeContentType(mqttProperty);
				break;
			case RESPONSE_TOPIC:
				minBytes += sizeResponseTopic(mqttProperty);
				break;
			case CORRELATION_DATA:
				minBytes += sizeCorrelationData(mqttProperty);
				break;
			case SUBSCRIPTION_IDENTIFIER:
				minBytes += sizeSubscriptionIdentifier(mqttProperty);
				break;
			case SESSION_EXPIRY_INTERVAL:
				userPropsBytes += sizeSessionExpiryInterval(mqttProperty);
				break;
			case ASSIGNED_CLIENT_IDENTIFIER:
				minBytes += sizeAssignedIdentifier(mqttProperty);
				break;
			case SERVER_KEEP_ALIVE:
				minBytes += sizeServerKeepAlive(mqttProperty);
				break;
			case AUTHENTICATION_METHOD:
				minBytes += sizeAuthMethod(mqttProperty);
				break;
			case AUTHENTICATION_DATA:
				minBytes += sizeAuthData(mqttProperty);
				break;
			case REQUEST_PROBLEM_INFORMATION:
				minBytes += sizeRequestProblemInformation(mqttProperty);
				break;
			case WILL_DELAY_INTERVAL:
				minBytes += sizeWillDelayInterval(mqttProperty);
				break;
			case REQUEST_RESPONSE_INFORMATION:
				minBytes += sizeRequestResponseInformation(mqttProperty);
				break;
			case RESPONSE_INFORMATION:
				minBytes += sizeResponseInformaiton(mqttProperty);
				break;
			case SERVER_REFERENCE:
				minBytes += sizeServerReference(mqttProperty);
				break;
			case REASON_STRING:
				reasonStringBytes += sizeReasonStringProp(mqttProperty);
				break;
			case RECEIVE_MAXIMUM:
				minBytes += sizeReceiveMaximum(mqttProperty);
				break;
			case TOPIC_ALIAS_MAXIMUM:
				minBytes += sizeTopicAliasMaximum(mqttProperty);
				break;
			case TOPIC_ALIAS:
				minBytes += sizeTopicAlias(mqttProperty);
				break;
			case MAXIMUM_QOS:
				minBytes += sizeMaximumQoS(mqttProperty);
				break;
			case RETAIN_AVAILABLE:
				minBytes += sizeRetainAvailable(mqttProperty);
				break;
			case USER_PROPERTY:
				userPropsBytes += sizeUserProp(mqttProperty);
				break;
			case MAXIMUM_PACKET_SIZE:
				minBytes += sizeMaximumPacketSize(mqttProperty);
				break;
			case WILDCARD_SUBSCRIPTION_AVAILABLE:
				minBytes += sizeWildcardSubscriptionAvailable(mqttProperty);
				break;
			case SUBSCRIPTION_IDENTIFIER_AVAILABLE:
				minBytes += sizeSubscriptionIdentifierAvailable(mqttProperty);
				break;
			case SHARED_SUBSCRIPTION_AVAILABLE:
				minBytes += sizeSharedSubscriptionAvailable(mqttProperty);
				break;
			}
		}
		// The Property Length is encoded as a Variable Byte Integer. The Property
		// Length does not include the bytes used to encode itself, but includes the
		// length of the Properties. If there are no properties, this MUST be indicated
		// by including a Property Length of zero [MQTT5-2.2.2-1].
		return new MqttPropertiesBytes(MqttMessageSizer.varIntBytes(minBytes) + minBytes, reasonStringBytes, userPropsBytes);
	}

	private <T> int sizePacketFormatIndicator(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.IntegerProperty;
		// 1 byte for encoding propertyId: (0x01) PacketFormatIndicator
		// 1 byte for encoding value 0|1
		return 2;
	}

	private <T> int sizeMessageExpiryInterval(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.IntegerProperty;
		// 1 byte for encoding propertyId: (0x02) MessageExpiryInterval
		// 4 byte5 for encoding expiry interval value
		return 5;
	}

	private <T> int sizeContentType(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.StringProperty;
		// 1 byte for encoding propertyId: (0x03) ContentType
		return 1 + utf8Bytes(((MqttProperties.StringProperty) mqttProp).value());
	}

	private <T> int sizeResponseTopic(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.StringProperty;
		// 1 byte for encoding propertyId: (0x08) ResponseTopic
		return 1 + utf8Bytes(((MqttProperties.StringProperty) mqttProp).value());
	}

	private <T> int sizeCorrelationData(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.BinaryProperty;
		// 1 byte for encoding propertyId: (0x09) CorrelationData
		return 1 + MqttMessageSizer.sizeBinary(((MqttProperties.BinaryProperty) mqttProp).value());
	}

	private <T> int sizeSubscriptionIdentifier(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.IntegerProperty;
		// 1 byte for encoding propertyId: (0x0B) SubscriptionIdentifier
		return 1 + MqttMessageSizer.varIntBytes(((MqttProperties.IntegerProperty) mqttProp).value());
	}

	private <T> int sizeSessionExpiryInterval(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.IntegerProperty;
		// 1 byte for encoding propertyId: (0x11) SessionExpiryInterval
		// 4 bytes for encoding SEI value
		return 5;
	}

	private <T> int sizeAssignedIdentifier(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.StringProperty;
		// 1 byte for encoding propertyId: (0x12) AssignedIdentifier
		return 1 + utf8Bytes(((MqttProperties.StringProperty) mqttProp).value());
	}

	private <T> int sizeServerKeepAlive(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.IntegerProperty;
		// 1 byte for encoding propertyId: (0x12) AssignedIdentifier
		// 2 bytes for encoding keepAlive value;
		return 3;
	}

	private <T> int sizeAuthMethod(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.StringProperty;
		// 1 byte for encoding propertyId: (0x15) AuthenticationMethod
		return 1 + MqttMessageSizer.sizeUTF8EncodedString((String) mqttProp.value());
	}

	private <T> int sizeAuthData(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.BinaryProperty;
		// 1 byte for encoding propertyId: (0x16) AuthenticationData
		return 1 + MqttMessageSizer.sizeBinary(((MqttProperties.BinaryProperty) mqttProp).value());
	}

	private <T> int sizeRequestProblemInformation(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.IntegerProperty;
		// 1 byte for encoding propertyId: (0x17) RequestProblemInformation
		// 1 byte for encoding 0|1
		return 2;
	}

	private <T> int sizeWillDelayInterval(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.IntegerProperty;
		// 1 byte for encoding propertyId: (0x18) WillDelayInterval
		// 4 bytes for encoding delay interval value
		return 5;
	}

	private <T> int sizeRequestResponseInformation(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.IntegerProperty;
		// 1 byte for encoding propertyId: (0x19) RequestResponseInformation
		// 1 byte for encoding 0|1
		return 2;
	}

	private <T> int sizeResponseInformaiton(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.StringProperty;
		// 1 byte for encoding propertyId: (0x1A) ResponseInformation
		return 1 + utf8Bytes(((MqttProperties.StringProperty) mqttProp).value());
	}

	private <T> int sizeServerReference(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.StringProperty;
		// 1 byte for encoding propertyId: (0x1C) ServerReference
		return 1 + utf8Bytes(((MqttProperties.StringProperty) mqttProp).value());
	}

	private <T> int sizeReasonStringProp(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.StringProperty;
		// 1 byte for encoding propertyId: (0x1F) ReasonString
		return 1 + MqttMessageSizer.sizeUTF8EncodedString((String) mqttProp.value());
	}

	private <T> int sizeReceiveMaximum(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.IntegerProperty;
		// 1 byte for encoding propertyId: (0x21) ReceiveMaximum
		// 2 bytes for encoding receive maximum value
		return 3;
	}

	private <T> int sizeTopicAliasMaximum(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.IntegerProperty;
		// 1 byte for encoding propertyId: (0x22) TopicAliasMaximum
		// 2 bytes for encoding topic alias maximum value
		return 3;
	}

	private <T> int sizeTopicAlias(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.IntegerProperty;
		// 1 byte for encoding propertyId: (0x23) TopicAlias
		// 2 bytes for encoding alias value
		return 3;
	}

	private <T> int sizeMaximumQoS(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.IntegerProperty;
		// 1 byte for encoding propertyId: (0x24) MaximumQoS
		// 1 bytes for encoding 0|1 value
		return 2;
	}

	private <T> int sizeRetainAvailable(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.IntegerProperty;
		// 1 byte for encoding propertyId: (0x25) RetainAvailable
		// 1 bytes for encoding 0|1 value
		return 2;
	}

	private <T> int sizeMaximumPacketSize(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.IntegerProperty;
		// 1 byte for encoding propertyId: (0x27) MaximumPacketSize
		// 4 bytes for encoding max packet size value
		return 5;
	}

	private <T> int sizeWildcardSubscriptionAvailable(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.IntegerProperty;
		// 1 byte for encoding propertyId: (0x28) WildcardSubscriptionAvailable
		// 1 byte for encoding 0|1 value
		return 2;
	}

	private <T> int sizeSubscriptionIdentifierAvailable(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.IntegerProperty;
		// 1 byte for encoding propertyId: (0x29) SubscriptionIdentifierAvailable
		// 1 byte for encoding 0|1 value
		return 2;

	}

	private <T> int sizeSharedSubscriptionAvailable(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.IntegerProperty;
		// 1 byte for encoding propertyId: (0x2A) SharedSubscriptionAvailable
		// 1 byte for encoding 0|1 value
		return 2;
	}

	private <T> int sizeUserProp(MqttProperties.MqttProperty<T> mqttProp) {
		assert mqttProp instanceof MqttProperties.UserProperties;
		int totalBytes = 0;
		for (MqttProperties.StringPair pair : ((MqttProperties.UserProperties) mqttProp).value()) {
			// 1 byte for encoding propertyId: (0x26) UserProperty
			totalBytes += 1;
			totalBytes += MqttMessageSizer.sizeUTF8EncodedString(pair.key);
			totalBytes += MqttMessageSizer.sizeUTF8EncodedString(pair.value);
		}
		return totalBytes;
	}
}