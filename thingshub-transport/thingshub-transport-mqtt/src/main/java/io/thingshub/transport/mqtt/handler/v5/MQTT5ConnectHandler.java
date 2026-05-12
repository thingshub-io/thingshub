package io.thingshub.transport.mqtt.handler.v5;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_ACCEPTED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_BANNED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_CLIENT_IDENTIFIER_NOT_VALID;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_IMPLEMENTATION_SPECIFIC;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_MALFORMED_PACKET;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED_5;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_PACKET_TOO_LARGE;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_PROTOCOL_ERROR;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_QOS_NOT_SUPPORTED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_QUOTA_EXCEEDED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_RETAIN_NOT_SUPPORTED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_TOPIC_NAME_INVALID;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_UNSPECIFIED_ERROR;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_UNSUPPORTED_PROTOCOL_VERSION;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.AUTHENTICATION_DATA;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.AUTHENTICATION_METHOD;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.CONTENT_TYPE;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.CORRELATION_DATA;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.MAXIMUM_PACKET_SIZE;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.MAXIMUM_QOS;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.PAYLOAD_FORMAT_INDICATOR;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.PUBLICATION_EXPIRY_INTERVAL;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.REASON_STRING;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.RECEIVE_MAXIMUM;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.REQUEST_PROBLEM_INFORMATION;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.REQUEST_RESPONSE_INFORMATION;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.RESPONSE_INFORMATION;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.RESPONSE_TOPIC;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.RETAIN_AVAILABLE;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.SERVER_KEEP_ALIVE;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.SESSION_EXPIRY_INTERVAL;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.SHARED_SUBSCRIPTION_AVAILABLE;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.SUBSCRIPTION_IDENTIFIER_AVAILABLE;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.TOPIC_ALIAS_MAXIMUM;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.USER_PROPERTY;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.WILDCARD_SUBSCRIPTION_AVAILABLE;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.WILL_DELAY_INTERVAL;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import javax.net.ssl.SSLPeerUnverifiedException;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttProperties.BinaryProperty;
import io.netty.handler.codec.mqtt.MqttProperties.IntegerProperty;
import io.netty.handler.codec.mqtt.MqttProperties.StringPair;
import io.netty.handler.codec.mqtt.MqttProperties.StringProperty;
import io.netty.handler.codec.mqtt.MqttProperties.UserProperty;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttReasonCodeAndPropertiesVariableHeader;
import io.netty.handler.ssl.SslHandler;
import io.thingshub.Broker;
import io.thingshub.config.TenantSettings;
import io.thingshub.service.model.ClientInfo;
import io.thingshub.subscribe.TopicUtils;
import io.thingshub.transport.authenticate.AuthRequest;
import io.thingshub.transport.authenticate.AuthRequest.AuthRequestBuilder;
import io.thingshub.transport.authenticate.AuthResult;
import io.thingshub.transport.authenticate.Authenticator;
import io.thingshub.transport.mqtt.MqttMessageSizer;
import io.thingshub.transport.mqtt.MqttTransportConfig;
import io.thingshub.transport.mqtt.handler.ConnectReject;
import io.thingshub.transport.mqtt.handler.MQTTAuthResult;
import io.thingshub.transport.mqtt.handler.MQTTConnectHandler;
import io.thingshub.transport.mqtt.handler.MQTTSessionHandler;
import io.thingshub.transport.mqtt.packet.LastWillTestament;
import io.thingshub.transport.utils.UTF8Utils;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * MQTT5 connect handler
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class MQTT5ConnectHandler extends MQTTConnectHandler {

	private boolean isAuthing;

	private boolean requestProblemInfo = true;

	private String authMethod;

	private final Authenticator authenticator;

	public MQTT5ConnectHandler(MqttTransportConfig mqttServerConfig) {
		super(mqttServerConfig);

		this.authenticator = Broker.getBean(Authenticator.class);
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		super.handlerAdded(ctx);
	}

	@Override
	protected ConnectReject check(MqttConnectMessage connectMessage) {
		final String clientId = connectMessage.payload().clientIdentifier();

		IntegerProperty propRPI = (IntegerProperty) connectMessage.variableHeader().properties().getProperty(REQUEST_PROBLEM_INFORMATION.value());
		if (propRPI != null) {
			this.requestProblemInfo = (propRPI.value() == 1);
		}

		if (!UTF8Utils.isWellFormed(clientId, false)) {
			MqttProperties props = new MqttProperties();
			props.add(new StringProperty(REASON_STRING.value(), "Malformed client identifier"));

			return new ConnectReject(MqttMessageBuilders.connAck().properties(props).returnCode(CONNECTION_REFUSED_CLIENT_IDENTIFIER_NOT_VALID).build(),
					"Malformed client identifier", false);
		}

		if (clientId.length() > MAX_CLIENT_ID_LENGTH) {
			MqttProperties props = new MqttProperties();
			props.add(new StringProperty(REASON_STRING.value(), "Max " + MAX_CLIENT_ID_LENGTH + " chars allowed"));

			return new ConnectReject(MqttMessageBuilders.connAck().properties(props).returnCode(CONNECTION_REFUSED_CLIENT_IDENTIFIER_NOT_VALID).build(),
					"Client identifier exceeds max length", false);
		}

		if (connectMessage.variableHeader().isCleanSession() && clientId.isEmpty()) {
			MqttProperties props = new MqttProperties();
			props.add(new StringProperty(REASON_STRING.value(), "Client identifier missing when CleanStart is set to true"));

			return new ConnectReject(MqttMessageBuilders.connAck().properties(props).returnCode(CONNECTION_REFUSED_CLIENT_IDENTIFIER_NOT_VALID).build(),
					"Client identifier missing when CleanStart is set to true", false);
		}

		if (connectMessage.variableHeader().hasUserName() && !UTF8Utils.isWellFormed(connectMessage.payload().userName(), false)) {
			MqttProperties props = new MqttProperties();
			props.add(new StringProperty(REASON_STRING.value(), "Malformed user name"));

			return new ConnectReject(MqttMessageBuilders.connAck().properties(props).returnCode(CONNECTION_REFUSED_MALFORMED_PACKET).build(), "Malformed user name", false);
		}

		StringProperty propAuthMehtod = (StringProperty) connectMessage.variableHeader().properties().getProperty(AUTHENTICATION_METHOD.value());
		BinaryProperty propAuthData = (BinaryProperty) connectMessage.variableHeader().properties().getProperty(AUTHENTICATION_DATA.value());
		if ((propAuthMehtod == null || Strings.isNullOrEmpty(propAuthMehtod.value())) && propAuthData != null) {
			MqttProperties props = new MqttProperties();
			props.add(new StringProperty(REASON_STRING.value(), "Missing auth method for authData"));

			return new ConnectReject(MqttMessageBuilders.connAck().properties(props).returnCode(CONNECTION_REFUSED_PROTOCOL_ERROR).build(), "Missing auth method for authData",
					false);
		}

		if (connectMessage.variableHeader().isWillFlag()) {
			if (!UTF8Utils.isWellFormed(connectMessage.payload().willTopic(), false)) {
				MqttProperties props = new MqttProperties();
				props.add(new StringProperty(REASON_STRING.value(), "Malformed will topic"));

				return new ConnectReject(MqttMessageBuilders.connAck().properties(props).returnCode(CONNECTION_REFUSED_MALFORMED_PACKET).build(), "Malformed will topic", false);
			}
		}

		return null;
	}

	@Override
	protected MQTTAuthResult authenticate(MqttConnectMessage connectMessage) {
		AuthRequest authRequest = buildAuthRequest(ctx.channel(), connectMessage);
		IntegerProperty propRPI = (IntegerProperty) connectMessage.variableHeader().properties().getProperty(REQUEST_PROBLEM_INFORMATION.value());
		if (propRPI != null) {
			this.requestProblemInfo = (propRPI.value() == 1);
		}

		AuthResult authResult = null;
		if (Strings.isNullOrEmpty(authRequest.authenticationMethod())) {
			authResult = authenticator.basicAuth(authRequest);
		} else {
			this.authMethod = authRequest.authenticationMethod();
			this.isAuthing = true;
			ctx.channel().config().setAutoRead(true);
			ctx.read();

			authResult = authenticator.extendedAuth(authRequest);
			this.isAuthing = false;
		}

		boolean isFarewell = false;
		MqttMessage reply = null;
		String reason = null;
		switch (authResult.resultCode()) {
		case OK -> {
			isFarewell = false;
		}
		case CONTINUE -> {
			MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.AUTH, false, MqttQoS.AT_MOST_ONCE, false, 0);
			MqttProperties mqttProperties = new MqttProperties();
			mqttProperties.add(new MqttProperties.StringProperty(AUTHENTICATION_METHOD.value(), authMethod));

			if (authResult.authData() != null) {
				mqttProperties.add(new MqttProperties.BinaryProperty(AUTHENTICATION_DATA.value(), authResult.authData()));
			}

			if (requestProblemInfo) {// [MQTT-3.1.2-29]
				if (Strings.isNullOrEmpty(authResult.reasonString())) {
					mqttProperties.add(new MqttProperties.StringProperty(REASON_STRING.value(), authResult.reasonString()));
				}

				if (authResult.userProperties() != null) {
					authResult.userProperties().forEach(userProperty -> mqttProperties.add(userProperty));
				}
			}

			MqttReasonCodeAndPropertiesVariableHeader variableHeader = new MqttReasonCodeAndPropertiesVariableHeader(MQTT5AuthReasonCode.CONTINUE.value(), mqttProperties);

			reply = new MqttMessage(fixedHeader, variableHeader);
		}
		case BAD_PASS -> {
			isFarewell = true;
			reply = MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD).build();
			reason = "Bad username or password";
		}
		case NOT_AUTHORIZED -> {
			isFarewell = true;
			reply = MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_NOT_AUTHORIZED_5).build();
			reason = "Client not authorized";
		}
		case BANNED -> {
			isFarewell = true;
			reply = MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_BANNED).build();
			reason = "Client banned";
		}
		default -> {
			isFarewell = true;
			reply = MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_UNSPECIFIED_ERROR).build();
			reason = "Unexpected error from auth service";
		}
		}
		;

		return new MQTTAuthResult(authResult.clientInfo(), isFarewell, reply, reason);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected AuthRequest buildAuthRequest(Channel channel, MqttConnectMessage connectMessage) {
		AuthRequestBuilder builder = AuthRequest.builder();
		SslHandler sslHandler = channel.pipeline().get(SslHandler.class);

		if (sslHandler != null) {
			try {
				Certificate[] certChains = sslHandler.engine().getSession().getPeerCertificates();
				if (certChains != null && certChains.length != 0) {
					X509Certificate cert = (X509Certificate) certChains[0];

					builder.cert(Base64.getEncoder().encode(cert.getEncoded()));
				}
			} catch (SSLPeerUnverifiedException | CertificateEncodingException ex) {
			}
		}

		builder.clientId(connectMessage.payload().clientIdentifier()).username(connectMessage.payload().userName())
				.password(new String(connectMessage.payload().passwordInBytes()));

		InetSocketAddress remoteAddr = (InetSocketAddress) channel.remoteAddress();
		if (remoteAddr != null) {
			builder.remotePort(remoteAddr.getPort());
			InetAddress ip = remoteAddr.getAddress();
			if (remoteAddr.getAddress() != null) {
				builder.remoteAddr(ip.getHostAddress());
			}
		}

		IntegerProperty propReqRespInfo = (IntegerProperty) connectMessage.variableHeader().properties().getProperty(REQUEST_RESPONSE_INFORMATION.value());
		if (propReqRespInfo != null) {
			builder.requestResponseInfomation(propReqRespInfo.value() == 1);
		}

		List<UserProperty> userProperties = (List<UserProperty>) connectMessage.variableHeader().properties().getProperties(USER_PROPERTY.value());
		builder.userProperties(userProperties);

		StringProperty proAuthenticationMethod = (StringProperty) connectMessage.variableHeader().properties().getProperty(AUTHENTICATION_METHOD.value());
		if (proAuthenticationMethod != null) {
			builder.authenticationMethod(proAuthenticationMethod.value());
		}

		BinaryProperty proAuthenticationData = (BinaryProperty) connectMessage.variableHeader().properties().getProperty(AUTHENTICATION_DATA.value());
		if (proAuthenticationData != null) {
			builder.authenticationData(proAuthenticationData.value());
		}

		return builder.build();
	}

	@Override
	protected ConnectReject onResourceLimit(String resourceName) {
		MqttProperties props = new MqttProperties();
		props.add(new StringProperty(REASON_STRING.value(), "Resource quota exceeded"));

		return new ConnectReject(MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_QUOTA_EXCEEDED).properties(props).build(), "Resource quota exceeded: " + resourceName,
				false);
	}

	@Override
	protected ConnectReject validate(MqttConnectMessage message, TenantSettings settings) {
		if (message.variableHeader().version() == 5 && !settings.isMqtt5Enabled()) {
			MqttProperties props = new MqttProperties();
			props.add(new StringProperty(REASON_STRING.value(), "MQTT5 not enabled"));

			return new ConnectReject(MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_UNSUPPORTED_PROTOCOL_VERSION).properties(props).build(),
					"Protocol violation: MQTT5 not enabled", false);
		}

		if (MqttMessageSizer.mqtt5().sizeOf(message) > settings.getMaxPacketSize()) {
			MqttProperties props = new MqttProperties();
			props.add(new StringProperty(REASON_STRING.value(), "Too large connect packet: max=" + settings.getMaxPacketSize()));

			return new ConnectReject(MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_PACKET_TOO_LARGE).properties(props).build(),
					"Protocol violation: too large packet", false);
		}

		IntegerProperty propMaxPacketSize = (IntegerProperty) message.variableHeader().properties().getProperty(MAXIMUM_PACKET_SIZE.value());
		if (propMaxPacketSize != null) {
			if (propMaxPacketSize.value() < 16) {
				MqttProperties props = new MqttProperties();
				props.add(new StringProperty(REASON_STRING.value(), "Invalid max packet size: " + propMaxPacketSize.value()));

				return new ConnectReject(MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_IMPLEMENTATION_SPECIFIC).properties(props).build(),
						"Protocol violation: invalid max packet size " + propMaxPacketSize.value(), false);
			}
			if (propMaxPacketSize.value() > settings.getMaxPacketSize()) {
				MqttProperties props = new MqttProperties();
				props.add(new StringProperty(REASON_STRING.value(), "Max packet size: " + settings.getMaxPacketSize()));

				return new ConnectReject(MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_IMPLEMENTATION_SPECIFIC).properties(props).build(),
						"Protocol violation: too large packet size - " + propMaxPacketSize.value(), false);
			}
		}

		IntegerProperty propTopicAliasMax = (IntegerProperty) message.variableHeader().properties().getProperty(TOPIC_ALIAS_MAXIMUM.value());
		if (propTopicAliasMax != null && propTopicAliasMax.value() > settings.getMaxTopicAlias()) {
			MqttProperties props = new MqttProperties();
			props.add(new StringProperty(REASON_STRING.value(), "Too large topic alias - " + settings.getMaxTopicAlias()));

			return new ConnectReject(MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_QUOTA_EXCEEDED).properties(props).build(),
					"Protocol violation: too large topic alias - " + propTopicAliasMax.value(), false);
		}

		if (message.variableHeader().isWillFlag()) {
			if (!TopicUtils.isValidTopic(message.payload().willTopic(), settings.getMaxTopicLevelLength(), settings.getMaxTopicLevels(), settings.getMaxTopicLength())) {
				MqttProperties props = new MqttProperties();
				props.add(new StringProperty(REASON_STRING.value(), "Will topic exceeds limits"));

				return new ConnectReject(MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_TOPIC_NAME_INVALID).properties(props).build(),
						"Protocol violation: will topic exceeds limits", false);
			}

			if (message.variableHeader().isWillRetain() && !settings.isRetainEnabled()) {
				return new ConnectReject(MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_RETAIN_NOT_SUPPORTED).build(), "Protocol violation: retain not supported",
						false);
			}

			if (message.variableHeader().willQos() > settings.getMaxMqttQoS().value()) {
				return new ConnectReject(MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_QOS_NOT_SUPPORTED).build(), "Protocol violation: will QoS not supported",
						false);
			}
		}

		return null;
	}

	@Override
	protected LastWillTestament getLastWillTestament(MqttConnectMessage connectMessage) {
		if (!connectMessage.variableHeader().isWillFlag()) {
			return null;
		}

		IntegerProperty propWillDelayInterval = (IntegerProperty) connectMessage.payload().willProperties().getProperty(WILL_DELAY_INTERVAL.value());
		IntegerProperty propPublicationExpiryInterval = (IntegerProperty) connectMessage.payload().willProperties().getProperty(PUBLICATION_EXPIRY_INTERVAL.value());
		IntegerProperty propPayloadFormatIndicator = (IntegerProperty) connectMessage.payload().willProperties().getProperty(PAYLOAD_FORMAT_INDICATOR.value());
		StringProperty propContentType = (StringProperty) connectMessage.payload().willProperties().getProperty(CONTENT_TYPE.value());
		StringProperty propResponseTopic = (StringProperty) connectMessage.payload().willProperties().getProperty(RESPONSE_TOPIC.value());
		BinaryProperty propCorrelationData = (BinaryProperty) connectMessage.payload().willProperties().getProperty(CORRELATION_DATA.value());

		@SuppressWarnings("unchecked")
		List<UserProperty> userPropertyList = (List<MqttProperties.UserProperty>) connectMessage.variableHeader().properties()
				.getProperties(MqttProperties.MqttPropertyType.USER_PROPERTY.value());
		List<StringPair> userProperties = null;
		if (!userPropertyList.isEmpty()) {
			userProperties = Lists.newArrayList();
			for (UserProperty up : userPropertyList) {
				userProperties.add(new StringPair(up.value().key, up.value().value));
			}
		}

		return LastWillTestament.builder() //
				.payload(connectMessage.payload().willMessageInBytes()) //
				.retain(connectMessage.variableHeader().isWillRetain()) //
				.topic(connectMessage.payload().willTopic()) //
				.delayInterval(propWillDelayInterval == null ? 0 : propWillDelayInterval.value()) //
				.qos(connectMessage.variableHeader().willQos()) //
				.expiryInterval(propPublicationExpiryInterval == null ? Integer.MAX_VALUE : propPublicationExpiryInterval.value())//
				.payloadFormatIndicator(propPayloadFormatIndicator == null ? false : (propPayloadFormatIndicator.value().intValue() == 1))//
				.contentType(propContentType != null ? propContentType.value() : null) //
				.responseTopic(propResponseTopic != null ? propResponseTopic.value() : null) //
				.correlationData(propCorrelationData != null ? propCorrelationData.value() : null) //
				.userProperties(userProperties) //
				.build();
	}

	@Override
	protected int getSessionExpiryInterval(MqttConnectMessage message, TenantSettings settings) {
		IntegerProperty propSessionExpiryInterval = (IntegerProperty) message.variableHeader().properties().getProperty(SESSION_EXPIRY_INTERVAL.value());
		return Math.min(propSessionExpiryInterval == null ? 0 : propSessionExpiryInterval.value(), settings.getMaxSessionExpiryInterval());
	}

	@Override
	protected MqttConnAckMessage buildConnAck(MqttConnectMessage connectMessage, TenantSettings tenantSettings, int keepAlive, int sessionExpiryInterval, boolean sessionPresent) {
		MqttProperties props = new MqttProperties();

		props.add(new IntegerProperty(MAXIMUM_PACKET_SIZE.value(), tenantSettings.getMaxPacketSize()));
		props.add(new IntegerProperty(WILDCARD_SUBSCRIPTION_AVAILABLE.value(), 1));
		props.add(new IntegerProperty(SUBSCRIPTION_IDENTIFIER_AVAILABLE.value(), 1));
		props.add(new IntegerProperty(SHARED_SUBSCRIPTION_AVAILABLE.value(), 1));
		props.add(new IntegerProperty(RETAIN_AVAILABLE.value(), tenantSettings.isRetainEnabled() ? 1 : 0));
		props.add(new IntegerProperty(TOPIC_ALIAS_MAXIMUM.value(), tenantSettings.getMaxTopicAlias()));
		props.add(new IntegerProperty(RECEIVE_MAXIMUM.value(), tenantSettings.getReceiveMaximum()));

		if (connectMessage.variableHeader().keepAliveTimeSeconds() != keepAlive) {
			props.add(new IntegerProperty(SERVER_KEEP_ALIVE.value(), keepAlive));
		}

		IntegerProperty propSessionExpiryInterval = (IntegerProperty) connectMessage.variableHeader().properties().getProperty(SESSION_EXPIRY_INTERVAL.value());
		if (propSessionExpiryInterval == null || propSessionExpiryInterval.value() != sessionExpiryInterval) {
			props.add(new IntegerProperty(SESSION_EXPIRY_INTERVAL.value(), sessionExpiryInterval));
		}

		if (tenantSettings.getMaxMqttQoS() != MqttQoS.EXACTLY_ONCE) {
			props.add(new IntegerProperty(MAXIMUM_QOS.value(), tenantSettings.getMaxMqttQoS().value()));
		}

		IntegerProperty propReqRespInformation = (IntegerProperty) connectMessage.variableHeader().properties().getProperty(REQUEST_RESPONSE_INFORMATION.value());
		if (propReqRespInformation != null && propReqRespInformation.value() == 1) {
			props.add(new StringProperty(RESPONSE_INFORMATION.value(), ""));// TODO
		}

		return MqttMessageBuilders.connAck().sessionPresent(sessionPresent).properties(props).returnCode(CONNECTION_ACCEPTED).build();
	}

	@Override
	protected int getMaxPacketSize(MqttConnectMessage message, TenantSettings settings) {
		IntegerProperty propMaxPacketSize = (IntegerProperty) message.variableHeader().properties().getProperty(MAXIMUM_PACKET_SIZE.value());

		return propMaxPacketSize != null ? propMaxPacketSize.value() : settings.getMaxPacketSize();
	}

	@Override
	protected MQTTSessionHandler buildSessionHandler(TenantSettings tenantSettings, ClientInfo clientInfo, int keepAlive, boolean sessionPresent, int sessionExpiryInterval,
			int receiveMaxium, LastWillTestament lwt) {
		return new MQTT5SessionHandler(tenantSettings, clientInfo, keepAlive, sessionPresent, sessionExpiryInterval, receiveMaxium, lwt, this.requestProblemInfo);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void handleMqttMessage(MqttMessage message) {
		MqttMessageType msgType = message.fixedHeader().messageType();
		if (isAuthing) {
			switch (msgType) {
			case AUTH -> rejectConnection(null, "Enhanced auth in progress", true);
			case DISCONNECT -> rejectConnection(null, "Enhanced auth abort by client", true);
			default -> rejectConnection(null, "Unexpected control packet during enhanced auth: " + msgType, true);// [MQTT-3.1.2-30]
			}
		} else {
			switch (msgType) {
			case AUTH -> {
				MQTT5AuthReasonCode reasonCode = MQTT5AuthReasonCode.of(((MqttReasonCodeAndPropertiesVariableHeader) message.variableHeader()).reasonCode());
				if (reasonCode != MQTT5AuthReasonCode.CONTINUE) {
					rejectConnection(null, "Invalid auth reason code: " + reasonCode, true);
					return;
				}

				MqttProperties authProperties = ((MqttReasonCodeAndPropertiesVariableHeader) message.variableHeader()).properties();
				StringProperty proAuthenticationMethod = (StringProperty) authProperties.getProperty(AUTHENTICATION_METHOD.value());
				if (!authMethod.equals(proAuthenticationMethod.value())) {
					rejectConnection(null, "Invalid auth method: " + proAuthenticationMethod.value(), true);
					return;
				}

				AuthRequestBuilder authRequestBuilder = AuthRequest.builder().isReAuth(false);

				if (proAuthenticationMethod != null) {
					authRequestBuilder.authenticationMethod(proAuthenticationMethod.value());
				}
				BinaryProperty proAuthenticationData = (BinaryProperty) authProperties.getProperty(AUTHENTICATION_DATA.value());
				if (proAuthenticationData != null) {
					authRequestBuilder.authenticationData(proAuthenticationData.value());
				}
				List<UserProperty> userProperties = (List<UserProperty>) authProperties.getProperties(USER_PROPERTY.value());
				if (userProperties != null && !userProperties.isEmpty()) {
					authRequestBuilder.userProperties(userProperties);
				}
				AuthResult authResult = authenticator.extendedAuth(authRequestBuilder.build());
				// TODO
			}
			case DISCONNECT -> rejectConnection(null, "Enhanced auth abort by client", true);
			default -> rejectConnection(null, "Unexpected control packet during enhanced auth: " + msgType, true);// [MQTT-3.1.2-30]
			}
		}
	}

	@Override
	protected int getReceiveMaxium(MqttConnectMessage message) {
		IntegerProperty propReceiveMaxium = (IntegerProperty) message.variableHeader().properties().getProperty(RECEIVE_MAXIMUM.value());
		return propReceiveMaxium != null ? propReceiveMaxium.value() : 65536;
	}

}