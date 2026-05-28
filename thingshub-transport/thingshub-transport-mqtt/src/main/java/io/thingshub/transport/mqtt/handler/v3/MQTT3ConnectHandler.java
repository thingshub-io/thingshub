package io.thingshub.transport.mqtt.handler.v3;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_ACCEPTED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;

import com.google.common.base.Strings;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.thingshub.Broker;
import io.thingshub.config.TenantSettings;
import io.thingshub.service.model.ClientInfo;
import io.thingshub.subscribe.TopicUtils;
import io.thingshub.transport.authenticate.AuthRequest;
import io.thingshub.transport.authenticate.AuthResult;
import io.thingshub.transport.authenticate.Authenticator;
import io.thingshub.transport.mqtt.MqttTransportConfig;
import io.thingshub.transport.mqtt.handler.ConnectReject;
import io.thingshub.transport.mqtt.handler.MQTTAuthResult;
import io.thingshub.transport.mqtt.handler.MQTTConnectHandler;
import io.thingshub.transport.mqtt.handler.MQTTSessionHandler;
import io.thingshub.transport.mqtt.packet.LastWillTestament;
import io.thingshub.transport.utils.UTF8Utils;

/**
 * <p>
 * MQTT3 connect handler
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public class MQTT3ConnectHandler extends MQTTConnectHandler {

	private final Authenticator authenticator;

	public MQTT3ConnectHandler(MqttTransportConfig mqttTransportConfig) {
		super(mqttTransportConfig);

		this.authenticator = Broker.getBean(Authenticator.class);
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		super.handlerAdded(ctx);
	}

	@Override
	protected ConnectReject check(MqttConnectMessage message) {
		String clientId = message.payload().clientIdentifier();
		if (Strings.isNullOrEmpty(clientId)) {
			if (!message.variableHeader().isCleanSession()) {
				return new ConnectReject(MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_IDENTIFIER_REJECTED).build(),
						"Client identifier missing while clean sesion is set to true", false);
			}
		}

		if (!UTF8Utils.isWellFormed(clientId, false)) {
			return new ConnectReject("Malformed client identifier");
		}

		if (clientId.length() > MAX_CLIENT_ID_LENGTH) {
			return new ConnectReject(MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_IDENTIFIER_REJECTED).build(),
					"Client identifier exceeds max length", false);
		}

		if (message.variableHeader().hasUserName() && !UTF8Utils.isWellFormed(message.payload().userName(), false)) {
			return new ConnectReject("Malformed user name");
		}

		if (message.variableHeader().isWillFlag() && !UTF8Utils.isWellFormed(message.payload().willTopic(), false)) {
			return new ConnectReject("Malformed will topic");
		}

		return null;
	}

	@Override
	protected MQTTAuthResult authenticate(MqttConnectMessage connectMessage) {
		AuthRequest authRequest = buildAuthRequest(ctx.channel(), connectMessage);
		AuthResult authResult = authenticator.basicAuth(authRequest);

		boolean isFarewell = false;
		MqttMessage reply = null;
		String reason = null;
		switch (authResult.resultCode()) {
		case OK -> {
			isFarewell = false;
		}
		case BAD_PASS -> {
			isFarewell = true;
			reply = MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD).build();
			reason = "Bad username or password";
		}
		case NOT_AUTHORIZED -> {
			isFarewell = true;
			reply = MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_NOT_AUTHORIZED).build();
			reason = "Client not authorized";
		}
		default -> {
			isFarewell = true;
			reply = MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_SERVER_UNAVAILABLE).build();
			reason = "Unexpected error from auth service";
		}
		}

		return new MQTTAuthResult(authResult.clientInfo(), isFarewell, reply, reason);

	}

	@Override
	protected ConnectReject onResourceLimit(String resourceName) {
		return new ConnectReject(MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_SERVER_UNAVAILABLE).build(),
				"Resource quota exceeded: " + resourceName, false);
	}

	@Override
	protected ConnectReject validate(MqttConnectMessage connectMessage, TenantSettings settings) {
		if (connectMessage.variableHeader().version() == 3 && !settings.isMqtt3Enabled()) {
			return new ConnectReject(MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION).build(),
					"Protocol violation: MQTT3.1 not enabled", false);
		}

		if (connectMessage.variableHeader().version() == 4 && !settings.isMqtt4Enabled()) {
			return new ConnectReject(MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION).build(),
					"Protocol violation: MQTT3.1.1 not enabled", false);
		}

		if (connectMessage.variableHeader().isWillFlag()) {
			if (!TopicUtils.isValidTopic(connectMessage.payload().willTopic(), settings.getMaxTopicLevelLength(), settings.getMaxTopicLevels(),
					settings.getMaxTopicLength())) {
				return new ConnectReject("Protocol violation: Will topic exceeds limits");
			}

			if (connectMessage.variableHeader().isWillRetain() && !settings.isRetainEnabled()) {
				return new ConnectReject("Protocol violation: Retain not supported");
			}

			if (connectMessage.variableHeader().willQos() > settings.getMaxMqttQoS().value()) {
				return new ConnectReject("Protocol violation: Will QoS not supported");
			}
		}

		return null;
	}

	@Override
	protected LastWillTestament getLastWillTestament(MqttConnectMessage connectMessage) {
		return connectMessage.variableHeader().isWillFlag() ? //
				LastWillTestament.builder() //
						.payload(connectMessage.payload().willMessageInBytes()) //
						.retain(connectMessage.variableHeader().isWillRetain()) //
						.topic(connectMessage.payload().willTopic()) //
						.delayInterval(0) //
						.qos(connectMessage.variableHeader().willQos()) //
						.expiryInterval(Integer.MAX_VALUE) //
						.build()
				: null;
	}

	@Override
	protected int getSessionExpiryInterval(MqttConnectMessage connectMessage, TenantSettings settings) {
		return connectMessage.variableHeader().isCleanSession() ? 0 : settings.getMaxSessionExpiryInterval();
	}

	@Override
	protected MqttConnAckMessage buildConnAck(MqttConnectMessage connectMessage, TenantSettings tenantSettings, int keepAlive, int sessionExpiryInterval,
			boolean sessionPresent) {
		return MqttMessageBuilders.connAck().sessionPresent(sessionPresent).returnCode(CONNECTION_ACCEPTED).build();
	}

	@Override
	protected int getMaxPacketSize(MqttConnectMessage connectMessage, TenantSettings settings) {
		return settings.getMaxPacketSize();
	}

	@Override
	protected int getReceiveMaxium(MqttConnectMessage message) {
		return 1 << 16;
	}

	@Override
	protected MQTTSessionHandler buildSessionHandler(TenantSettings tenantSettings, ClientInfo clientInfo, int keepAlive, boolean sessionPresent,
			int sessionExpiryInterval, int receiveMaxium, LastWillTestament lwt) {
		return new MQTT3SessionHandler(tenantSettings, clientInfo, keepAlive, sessionPresent, sessionExpiryInterval, receiveMaxium, lwt);

	}

	@Override
	protected void handleMqttMessage(MqttMessage message) {
		// will never happen, do nothing
	}

}