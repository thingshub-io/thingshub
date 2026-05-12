package io.thingshub.transport.mqtt.handler;

import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.AUTHENTICATION_DATA;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.AUTHENTICATION_METHOD;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.REQUEST_RESPONSE_INFORMATION;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.USER_PROPERTY;
import static io.thingshub.transport.ChannelContextWrapper.ATTRIBUTE_CLIENT_ID;
import static io.thingshub.transport.ChannelContextWrapper.ATTRIBUTE_CLIENT_TYPE;
import static io.thingshub.transport.ChannelContextWrapper.ATTRIBUTE_CURRENT_SERVER_NAME;
import static io.thingshub.transport.ChannelContextWrapper.ATTRIBUTE_PRODUCT_CODE;
import static io.thingshub.transport.ChannelContextWrapper.ATTRIBUTE_SCRIPT_LANG;
import static io.thingshub.transport.throttler.ORCondition.or;
import static io.thingshub.transport.throttler.ResourceType.TOTAL_CONNECTIONS;
import static io.thingshub.transport.throttler.ResourceType.TOTAL_CONNECT_PER_SECOND;
import static io.thingshub.transport.throttler.ResourceType.TOTAL_SESSION_MEMORY_BYTES;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.slf4j.MDC;

import com.google.common.base.Strings;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttProperties.BinaryProperty;
import io.netty.handler.codec.mqtt.MqttProperties.IntegerProperty;
import io.netty.handler.codec.mqtt.MqttProperties.StringProperty;
import io.netty.handler.codec.mqtt.MqttProperties.UserProperty;
import io.netty.handler.ssl.SslHandler;
import io.thingshub.Broker;
import io.thingshub.commons.model.StreamDirection;
import io.thingshub.config.TenantSettings;
import io.thingshub.entity.Device;
import io.thingshub.entity.ScriptInfo;
import io.thingshub.service.DeviceService;
import io.thingshub.service.ScriptService;
import io.thingshub.service.model.ClientInfo;
import io.thingshub.service.model.ClientType;
import io.thingshub.transport.SessionManager;
import io.thingshub.transport.authenticate.AuthRequest;
import io.thingshub.transport.authenticate.AuthRequest.AuthRequestBuilder;
import io.thingshub.transport.mqtt.MqttTransportConfig;
import io.thingshub.transport.mqtt.codec.MessageTransformDecoder;
import io.thingshub.transport.mqtt.codec.MessageTransformEncoder;
import io.thingshub.transport.mqtt.packet.LastWillTestament;
import io.thingshub.transport.throttler.Condition;
import io.thingshub.transport.throttler.DirectMemPressureCondition;
import io.thingshub.transport.throttler.HeapMemPressureCondition;
import io.thingshub.transport.throttler.InboundResourceCondition;
import io.thingshub.transport.throttler.ResourceThrottler;
import io.thingshub.transport.throttler.SlowdownInboundHandler;
import io.thingshub.transport.utils.TaskTracker;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * MQTT connect handler
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public abstract class MQTTConnectHandler extends ChannelDuplexHandler {

	protected static final int MAX_CLIENT_ID_LENGTH = 65535;

	protected final MqttTransportConfig mqttServerConfig;

	private final ResourceThrottler resourceThrottler;

	private final SessionManager sessionManager;

	private final ScriptService scriptService;

	private final DeviceService deviceService;

	protected ChannelHandlerContext ctx;

	private boolean isLeaving;

	public MQTTConnectHandler(MqttTransportConfig mqttServerConfig) {
		this.mqttServerConfig = mqttServerConfig;

		this.sessionManager = Broker.getBean(SessionManager.class);
		this.resourceThrottler = Broker.getBean(ResourceThrottler.class);
		this.scriptService = Broker.getBean(ScriptService.class);
		this.deviceService = Broker.getBean(DeviceService.class);
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		TaskTracker.getInstance().stop();
		ctx.fireChannelInactive();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		setLoggerMdc(ctx, StreamDirection.UP, MqttMessageType.CONNECT.name());
		log.error("Client channel throwed exception and forced to close. ctx: {}, cause:", ctx, cause);
		ctx.channel().close();
	}

	private void setLoggerMdc(ChannelHandlerContext ctx, StreamDirection streamDirection, String msgName) {
		String clientId = ctx.channel().attr(ATTRIBUTE_CLIENT_ID).get();
		InetSocketAddress remoteAddr = (InetSocketAddress) ctx.channel().remoteAddress();
		String clientAddr = null;
		if (remoteAddr != null) {
			InetAddress ip = remoteAddr.getAddress();
			if (remoteAddr.getAddress() != null) {
				clientAddr = ip.getHostAddress();
			}
		}

		MDC.put("clientId", clientId);
		MDC.put("clientAddr", clientAddr);
		MDC.put("protocol", "MQTT");
		MDC.put("streamDirection", streamDirection.name());
		MDC.put("serverAddr", Broker.BROKER_ADDR);
		MDC.put("packetId", "---");
		MDC.put("msgName", msgName);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		MqttMessage mqttMessage = (MqttMessage) msg;

		if (mqttMessage.fixedHeader().messageType() == MqttMessageType.CONNECT) {
			MqttConnectMessage connectMessage = (MqttConnectMessage) msg;
			ConnectReject rejection4Check = check(connectMessage);
			if (rejection4Check != null) {
				rejectConnection(rejection4Check.farewell(), rejection4Check.reason(), rejection4Check.goawayImmediately());
				return;
			}

			TaskTracker.getInstance().track(CompletableFuture.supplyAsync(() -> authenticate(connectMessage), ctx.executor())).thenAccept(result -> {
				String tenant = result.clientInfo().tenant();
				ClientType clientType = result.clientInfo().clientType();
				if (result.isFarewell()) {
					rejectConnection(result.reply(), result.reason(), false);

					return;
				}

				if (!resourceThrottler.hasResource(tenant, TOTAL_CONNECTIONS)) {
					ConnectReject rejection4ResourceThrottler = onResourceLimit(TOTAL_CONNECTIONS.name());
					rejectConnection(rejection4ResourceThrottler.farewell(), rejection4ResourceThrottler.reason(),
							rejection4ResourceThrottler.goawayImmediately());
					return;
				}
				if (!resourceThrottler.hasResource(tenant, TOTAL_SESSION_MEMORY_BYTES)) {
					ConnectReject rejection4ResourceThrottler = onResourceLimit(TOTAL_SESSION_MEMORY_BYTES.name());
					rejectConnection(rejection4ResourceThrottler.farewell(), rejection4ResourceThrottler.reason(),
							rejection4ResourceThrottler.goawayImmediately());
					return;
				}
				if (!resourceThrottler.hasResource(tenant, TOTAL_CONNECT_PER_SECOND)) {
					ConnectReject rejection4ResourceThrottler = onResourceLimit(TOTAL_CONNECT_PER_SECOND.name());
					rejectConnection(rejection4ResourceThrottler.farewell(), rejection4ResourceThrottler.reason(),
							rejection4ResourceThrottler.goawayImmediately());
					return;
				}

				TenantSettings tenantSettings = TenantSettings.builder().tenant(tenant).build();
				ConnectReject rejection4Validation = validate(connectMessage, tenantSettings);
				if (rejection4Validation != null) {
					rejectConnection(rejection4Validation.farewell(), rejection4Validation.reason(), rejection4Validation.goawayImmediately());
					return;
				}

				int keepAlive = connectMessage.variableHeader().keepAliveTimeSeconds();
				if (keepAlive == 0) {
					keepAlive = mqttServerConfig.getKeepAlive();
				}
				keepAlive = Math.max(5, keepAlive);
				keepAlive = Math.min(keepAlive, 2 * 60 * 60);// max keep alive
				int finalKeeplAlive = keepAlive;

				LastWillTestament lwt = getLastWillTestament(connectMessage);

				String clientId = connectMessage.payload().clientIdentifier();
				ctx.channel().attr(ATTRIBUTE_CLIENT_ID).set(clientId);
				ctx.channel().attr(ATTRIBUTE_CLIENT_TYPE).set(clientType);
				ctx.channel().attr(ATTRIBUTE_CURRENT_SERVER_NAME).set(mqttServerConfig.getName());

				ScriptInfo transformScript = null;
				if (clientType == ClientType.DEVICE) {
					Device device = deviceService.getBySn(clientId);

					if (device != null) {
						ctx.channel().attr(ATTRIBUTE_PRODUCT_CODE).set(device.getProductCode());

						transformScript = scriptService.getScript(device.getProductCode());
						if (transformScript != null) {
							ctx.channel().attr(ATTRIBUTE_SCRIPT_LANG).set(transformScript.getLang());
						}
					}
				}

				Condition slowdownCondition = or(DirectMemPressureCondition.INSTANCE, HeapMemPressureCondition.INSTANCE,
						new InboundResourceCondition(resourceThrottler, tenant));
				SlowdownInboundHandler slowdownHandler = new SlowdownInboundHandler(slowdownCondition);
				ctx.pipeline().addFirst(ctx.executor(), SlowdownInboundHandler.class.getSimpleName(), slowdownHandler);

				MessageTransformDecoder messageTransformDecoder = Broker.getBean(MessageTransformDecoder.class);
				ctx.pipeline().addAfter(ctx.executor(), MqttDecoder.class.getSimpleName(), MessageTransformDecoder.class.getSimpleName(),
						messageTransformDecoder);
				MessageTransformEncoder messageTransformEncoder = Broker.getBean(MessageTransformEncoder.class);
				ctx.pipeline().addBefore(ctx.executor(), this.getClass().getSimpleName(), MessageTransformEncoder.class.getSimpleName(),
						messageTransformEncoder);

				boolean sessionPresent;
				if (connectMessage.variableHeader().isCleanSession()) {
					sessionPresent = false;
				} else {
					sessionPresent = sessionManager.getClientSession(clientId) != null ? true : false;
				}
				if (!sessionPresent) {
					sessionManager.clean(clientId);
				}

				int sessionExpiryInterval = getSessionExpiryInterval(connectMessage, tenantSettings);
				int receiveMaxium = getReceiveMaxium(connectMessage);
				MQTTSessionHandler sessionHandler = buildSessionHandler(tenantSettings, result.clientInfo(), finalKeeplAlive, sessionPresent,
						sessionExpiryInterval, receiveMaxium, lwt);
				sessionHandler.onInitialized(v -> {
					setLoggerMdc(ctx, StreamDirection.UP, MqttMessageType.CONNECT.name());
					log.info("Client connected");

					final AtomicReference<Boolean> connAckedRef = new AtomicReference<>(Boolean.FALSE);

					MqttConnAckMessage connAck = buildConnAck(connectMessage, tenantSettings, finalKeeplAlive, sessionExpiryInterval, sessionPresent);
					ctx.writeAndFlush(connAck).addListener(new ChannelFutureListener() {

						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							if (!future.isSuccess()) {
								setLoggerMdc(ctx, StreamDirection.DOWN, "CONNACK");
								log.error("Sending ConnAck message to client failed");
								if (future.cause() != null) {
									log.error("", future.cause());
								}
							} else {
								connAckedRef.set(Boolean.TRUE);
							}
						}
					}).awaitUninterruptibly(15, TimeUnit.SECONDS);

					return connAckedRef.get();
				});

				ctx.pipeline().replace(this, sessionHandler.getClass().getSimpleName(), sessionHandler);

				return;
			}).exceptionally(e -> {
				rejectConnection(null, "Unexpected error from auth service, error: {}" + e.getMessage(), false);

				return null;
			});
		} else {
			if (mqttMessage.decoderResult().isSuccess()) {
				handleMqttMessage(mqttMessage);
			} else {
				rejectConnection(null, "Protocol error: " + mqttMessage.decoderResult().cause().getMessage(), false);
			}
		}
	}

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

	protected void rejectConnection(MqttMessage farewell, String reason, boolean goawayImmediately) {
		assert ctx.executor().inEventLoop();
		if (isLeaving) {
			return;
		}

		setLoggerMdc(ctx, StreamDirection.UP, MqttMessageType.CONNECT.name());
		if (!Strings.isNullOrEmpty(reason)) {
			log.error(reason);
		}

		isLeaving = true;

		if (!ctx.channel().isActive()) {
			return;
		}

		Runnable goawayTask = () -> {
			if (farewell != null) {
				ctx.writeAndFlush(farewell).addListener(ChannelFutureListener.CLOSE);
			} else {
				ctx.channel().close();
			}
		};

		if (goawayImmediately) {
			goawayTask.run();
		} else {
			ctx.executor().schedule(goawayTask, ThreadLocalRandom.current().nextInt(100, 3000), TimeUnit.MILLISECONDS);
		}
	}

	protected abstract ConnectReject check(MqttConnectMessage connectMessage);

	protected abstract MQTTAuthResult authenticate(MqttConnectMessage connectMessage);

	protected abstract ConnectReject onResourceLimit(String resourceName);

	protected abstract ConnectReject validate(MqttConnectMessage connectMessage, TenantSettings tenantSettings);

	protected abstract LastWillTestament getLastWillTestament(MqttConnectMessage connectMessage);

	protected abstract int getSessionExpiryInterval(MqttConnectMessage connectMessage, TenantSettings tenantSettings);

	protected abstract int getMaxPacketSize(MqttConnectMessage message, TenantSettings settings);

	protected abstract int getReceiveMaxium(MqttConnectMessage message);

	protected abstract MQTTSessionHandler buildSessionHandler(TenantSettings tenantSettings, ClientInfo clientInfo, int keepAlive, boolean sessionPresent,
			int sessionExpiryInterval, int receiveMaxium, LastWillTestament lwt);

	protected abstract void handleMqttMessage(MqttMessage message);

	protected abstract MqttConnAckMessage buildConnAck(MqttConnectMessage connectMessage, TenantSettings settings, int keepAlive, int sessionExpiryInterval,
			boolean sessionPresent);

}