package io.thingshub.transport.mqtt.handler;

import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.REASON_STRING;
import static io.thingshub.transport.ChannelContextWrapper.ATTRIBUTE_CURRENT_SERVER_NAME;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.MDC;

import com.alibaba.fastjson2.JSON;
import com.google.common.base.Ticker;

import cn.hutool.core.date.DateUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttProperties.MqttProperty;
import io.netty.handler.codec.mqtt.MqttProperties.StringProperty;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttReasonCodeAndPropertiesVariableHeader;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.thingshub.Broker;
import io.thingshub.commons.StreamDirection;
import io.thingshub.commons.ThingshubMessage;
import io.thingshub.config.TenantSettings;
import io.thingshub.entity.Connection;
import io.thingshub.schedule.Scheduler;
import io.thingshub.service.base.IdGenerator;
import io.thingshub.service.model.ClientInfo;
import io.thingshub.service.model.Delivery;
import io.thingshub.subscribe.ClientSubscribe;
import io.thingshub.subscribe.ClientUnsubscribe;
import io.thingshub.subscribe.TopicUtils;
import io.thingshub.transport.ChannelContextWrapper;
import io.thingshub.transport.ConnectionManager;
import io.thingshub.transport.DeliveryReader;
import io.thingshub.transport.MessageRouter;
import io.thingshub.transport.SessionManager;
import io.thingshub.transport.Transport;
import io.thingshub.transport.TransportPacket;
import io.thingshub.transport.mqtt.MqttChannelContext;
import io.thingshub.transport.mqtt.handler.ValidateMessageResult.Action;
import io.thingshub.transport.mqtt.handler.v5.MQTT5DisconnectReasonCode;
import io.thingshub.transport.mqtt.packet.DisconnectPacket;
import io.thingshub.transport.mqtt.packet.LastWillTestament;
import io.thingshub.transport.mqtt.packet.PingPacket;
import io.thingshub.transport.mqtt.packet.PubAckPacket;
import io.thingshub.transport.mqtt.packet.PubCompPacket;
import io.thingshub.transport.mqtt.packet.PubRecPacket;
import io.thingshub.transport.mqtt.packet.PubRelPacket;
import io.thingshub.transport.mqtt.packet.PublishPacket;
import io.thingshub.transport.mqtt.packet.SubscribePacket;
import io.thingshub.transport.mqtt.packet.UnsubscribePacket;
import io.thingshub.transport.mqtt.service.LastWill;
import io.thingshub.transport.mqtt.service.LastWillService;
import io.thingshub.transport.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * MQTT session handler
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public abstract class MQTTSessionHandler extends ChannelDuplexHandler {

	public static final Map<String, MqttChannelContext> CHANNEL_CONTEXTS = new ConcurrentHashMap<>();

	private final ConnectionManager connectionManager;

	private final SessionManager sessionManager;

	private final DeliveryReader deliveryReader;

	private final MessageRouter messageRouter;

	private final LastWillService lastWillService;

	private final IdGenerator idGenerator;

	private final Scheduler scheduler;

	protected final TenantSettings tenantSettings;

	protected final ClientInfo clientInfo;

	private final int keepAlive;

	private final int receiveMaxium;

	private final boolean sessionPresent;

	private final long idleTimeout;

	private int sessionExpiryInterval;

	private int receivingCount = 0;

	private boolean requestProblemInfo;

	protected MqttChannelContext mqttChannelContextWrapper;

	private LastWillTestament lwt;

	private long lastActivateAt;

	private long sessionId;

	private boolean isLeaving;

//	protected final AtomicLong memoryUsage;//TODO

	private ScheduledFuture<?> idleTimeoutCheckingTask;

	private Function<Void, Boolean> initCallback;

	private void onKick(String clientId, String channelId) {
		MqttChannelContext ctx = CHANNEL_CONTEXTS.get(clientId);
		if (ctx == null || !ctx.channel().id().asLongText().equals(channelId)) {
			return;
		}

		setLoggerMdc(ctx.getClientId(), ctx.getClientAddr(), StreamDirection.DOWN, "---", "KICK");
		log.info("Client with channel id [{}] in node [{}] is kicked", ctx.channel().id().asLongText(), Broker.BROKER_ADDR);

		if (Integer.parseInt(ctx.getProtocolVersion()) < 5) {
			ctx.goAway(true);
		} else {
			MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);

			MqttProperties props = new MqttProperties();
			props.add(new StringProperty(REASON_STRING.value(), "Address: " + Broker.BROKER_ADDR));
			MqttReasonCodeAndPropertiesVariableHeader variableHeader = new MqttReasonCodeAndPropertiesVariableHeader(MQTT5DisconnectReasonCode.SessionTakenOver.value(), props);

			ctx.goAwayWithFarewell(new MqttMessage(fixedHeader, variableHeader), true);
		}
	}

	public void setLoggerMdc(String clientId, String clientAddr, StreamDirection streamDirection, String packetId, String msgName) {
		MDC.put("clientId", clientId);
		MDC.put("clientAddr", clientAddr);
		MDC.put("protocol", "MQTT");
		MDC.put("streamDirection", streamDirection.name());
		MDC.put("serverAddr", Broker.BROKER_ADDR);
		MDC.put("packetId", packetId);
		MDC.put("msgName", msgName);
	}

	public MQTTSessionHandler(TenantSettings tenantSettings, ClientInfo clientInfo, int keepAlive, boolean sessionPresent, int sessionExpiryInterval, int receiveMaxium,
			LastWillTestament lwt, boolean requestProblemInfo) {
		this.tenantSettings = tenantSettings;
		this.clientInfo = clientInfo;
		this.keepAlive = keepAlive;
		this.sessionPresent = sessionPresent;
		this.sessionExpiryInterval = sessionExpiryInterval;
		this.receiveMaxium = receiveMaxium;
		this.lwt = lwt;
		this.idleTimeout = Duration.ofMillis(keepAlive * 1500L).toNanos();
		this.requestProblemInfo = requestProblemInfo;

		this.connectionManager = Broker.getBean(ConnectionManager.class);
		this.sessionManager = Broker.getBean(SessionManager.class);
		this.lastWillService = Broker.getBean(LastWillService.class);
		this.deliveryReader = Broker.getBean(DeliveryReader.class);
		this.messageRouter = Broker.getBean(MessageRouter.class);
		this.idGenerator = Broker.getBean(IdGenerator.class);
		this.scheduler = Broker.getBean(Scheduler.class);
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		connectionManager.kick(clientInfo.clientId(), this::onKick);

		ChannelTrafficShapingHandler trafficShapingHandler = ctx.channel().pipeline().get(ChannelTrafficShapingHandler.class);
		trafficShapingHandler.setReadLimit(tenantSettings.getInboundBandwidth());
		trafficShapingHandler.setWriteLimit(tenantSettings.getOutboundBandwidth());
		trafficShapingHandler.setMaxWriteSize(tenantSettings.getOutboundBandwidth());

		ctx.channel().pipeline().replace(ctx.pipeline().get(MqttDecoder.class.getSimpleName()), MqttDecoder.class.getSimpleName(),
				new MqttDecoder(tenantSettings.getMaxPacketSize()));
		if (tenantSettings.getMaxPacketSize() > ctx.channel().config().getWriteBufferHighWaterMark()) {
			ctx.channel().config().setWriteBufferHighWaterMark(tenantSettings.getMaxPacketSize() + 1024);
			ctx.channel().config().setWriteBufferLowWaterMark(tenantSettings.getMaxPacketSize() / 2);
		}

		InetSocketAddress remoteAddr = (InetSocketAddress) ctx.channel().remoteAddress();
		String clientAddr = null;
		if (remoteAddr != null) {
			InetAddress ip = remoteAddr.getAddress();
			if (remoteAddr.getAddress() != null) {
				clientAddr = ip.getHostAddress();
			}
		}
		mqttChannelContextWrapper = new MqttChannelContext(ctx, tenantSettings, clientInfo.username(), clientInfo.clientId(), clientAddr, keepAlive, requestProblemInfo,
				getProtocolVersion(), sessionExpiryInterval);
		lastActivateAt = Ticker.systemTicker().read();
		CHANNEL_CONTEXTS.put(clientInfo.clientId(), mqttChannelContextWrapper);

		connectionManager.add(buildConnectionInfo(mqttChannelContextWrapper));
		if (!sessionPresent) {
			sessionId = sessionManager.create(clientInfo.clientId(), Broker.getConsistentId(), sessionExpiryInterval);
		} else {
			sessionId = sessionManager.resume(clientInfo.clientId(), Broker.getConsistentId(), sessionExpiryInterval);
		}

		idleTimeoutCheckingTask = ctx.executor().scheduleAtFixedRate(this::checkIdleTimout, idleTimeout, idleTimeout, TimeUnit.NANOSECONDS);
		mqttChannelContextWrapper.addFgTask(CompletableFuture.runAsync(() -> {
			deliveryReader.start(clientInfo.clientId(), receiveMaxium, this::sendDelivery);
		}, ctx.executor()));

		if (initCallback != null) {
			Boolean isCallbackSuccess = initCallback.apply(null);
			if (isCallbackSuccess) {
				ctx.channel().config().setAutoRead(true);
				ctx.read();
			}
		}
	}

	private void sendDelivery(Delivery delivery) {
		MqttChannelContext ctx = CHANNEL_CONTEXTS.get(delivery.receiverId());
		if (ctx == null) {
			return;
		}

		if (!ctx.channel().isActive()) {
			setLoggerMdc(ctx.getClientId(), ctx.getClientAddr(), StreamDirection.DOWN, "---", "PUBLISH");
			log.error("Channel is inactive and sending PUBLISH packet is dropped. Delivery id: {}", delivery.id());

			deliveryReader.requeue(ctx.getClientId(), delivery);

			return;
		}
		if (!ctx.channel().isWritable()) {
			setLoggerMdc(ctx.getClientId(), ctx.getClientAddr(), StreamDirection.DOWN, "---", "PUBLISH");
			log.error("Channel is not writable and sending PUBLISH packet is dropped. Delivery id: {}", delivery.id());

			deliveryReader.requeue(ctx.getClientId(), delivery);
			return;
		}

//		ctx.addFgTask(CompletableFuture.runAsync(() -> {
		// TODO check if resource is exhausted

		Integer pubQos = (Integer) delivery.sendProps().get("qos");
		Integer subQos = (Integer) delivery.recProps().get("qos");
		int theQos = Math.min(pubQos == null ? MqttQoS.AT_LEAST_ONCE.value() : pubQos, subQos == null ? MqttQoS.AT_LEAST_ONCE.value() : subQos);
		MqttQoS mqttQos = MqttQoS.valueOf(theQos);

		switch (mqttQos) {
		case AT_MOST_ONCE -> {
			// TODO check pub QoS0 permission
			if (log.isDebugEnabled()) {
				setLoggerMdc(ctx.getClientId(), ctx.getClientAddr(), StreamDirection.DOWN, "0", "PUBLISH");
				log.debug("Server send PUBLISH packet, Delivery id: {}, QoS: {}", delivery.id(), mqttQos.value());
			}

			MqttPublishMessage publishMessage = buildPublishMessageFromDelivery(0, mqttQos, delivery);
			if (publishMessage != null) {
				ctx.writeAndFlush(publishMessage).addListener(new ChannelFutureListener() {

					@Override
					public void operationComplete(ChannelFuture future) throws Exception {

						if (future.isSuccess()) {
							deliveryReader.ackWriting(ctx.getClientId(), delivery.id().longValue());

							setLoggerMdc(ctx.getClientId(), ctx.getClientAddr(), StreamDirection.DOWN, "0", "PUBLISH");
							log.debug("Send PUBLISH packet successfully. Delivery id: {}, QoS: {}", delivery.id(), mqttQos.value());
						} else {
							setLoggerMdc(ctx.getClientId(), ctx.getClientAddr(), StreamDirection.DOWN, "0", "PUBLISH");
							log.error("Send PUBLISH packet error. Delivery id: {}, QoS: {}", delivery.id(), mqttQos.value());
							if (future.cause() != null) {
								log.error("", future.cause());
							}
						}
					}

				});
			}
		}
		case EXACTLY_ONCE, AT_LEAST_ONCE -> {
			String channelId = ctx.channel().id().asLongText();
			int packetId = ctx.nextPacketId();
			MqttChannelContext.bufferOutgoing(channelId.concat("_" + packetId).concat("_delivery"), delivery.id());

			if (log.isDebugEnabled()) {
				setLoggerMdc(ctx.getClientId(), ctx.getClientAddr(), StreamDirection.DOWN, String.valueOf(packetId), "PUBLISH");
				log.debug("Server send PUBLISH packet. Delivery id: {}, QoS: {}", delivery.id(), mqttQos.value());
			}

			String taskId = packetId + "";
			int resendTimes = ctx.getTenantSettings().getMaxResendTimes();
			io.thingshub.schedule.Action repubAction = () -> {
				MqttPublishMessage repubMessage = buildPublishMessageFromDelivery(packetId, mqttQos, delivery);
				if (repubMessage != null) {
					ctx.writeAndFlush(repubMessage).addListener(new ChannelFutureListener() {

						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							if (!future.isSuccess()) {
								setLoggerMdc(ctx.getClientId(), ctx.getClientAddr(), StreamDirection.DOWN, String.valueOf(packetId), "PUBLISH");
								log.error("Resend PUBLISH packet error. Delivery id: {}, QoS: {}", delivery.id(), mqttQos.value());

								if (future.cause() != null) {
									log.error("", future.cause());
								}
							}
						}

					});
				}
			};

			MqttPublishMessage immediatePubMsg = buildPublishMessageFromDelivery(packetId, mqttQos, delivery);
			if (immediatePubMsg != null) {
				ctx.writeAndFlush(immediatePubMsg).addListener(new ChannelFutureListener() {

					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						Broker.getBean(Scheduler.class).addTask(channelId, taskId, repubAction, 2000L, resendTimes);

						if (!future.isSuccess()) {
							setLoggerMdc(ctx.getClientId(), ctx.getClientAddr(), StreamDirection.DOWN, String.valueOf(packetId), "PUBLISH");
							log.error("Send PUBLISH packet error and will delay resending packet");
							if (future.cause() != null) {
								log.error("", future.cause());
							}
						}
					}

				});
			}
		}
		default -> log.error("Invalid qos level when sending delivery");
		}
//		}, ctx.executor()).exceptionally(e -> {
//			log.error("", e);
//
//			return null;
//		}));
	}

	private void checkIdleTimout() {
		if ((Ticker.systemTicker().read() - lastActivateAt) > idleTimeout) {
			setLoggerMdc(mqttChannelContextWrapper.getClientId(), mqttChannelContextWrapper.getClientAddr(), StreamDirection.DOWN, "---", "IDLE_TIMEOUT");
			log.warn("Client idle timeout and forced to close", clientInfo.clientId());

			idleTimeoutCheckingTask.cancel(true);
			mqttChannelContextWrapper.channel().config().setAutoRead(false);
			mqttChannelContextWrapper.close();
		}
	}

	private Connection buildConnectionInfo(MqttChannelContext ctx) {
		String channelId = ctx.channel().id().asLongText();
		String serverName = ctx.channel().attr(ATTRIBUTE_CURRENT_SERVER_NAME).get();

		Connection connection = new Connection();
		connection.setTenantId(tenantSettings.getTenant());
		connection.setClientId(clientInfo.clientId());
		connection.setClientType(clientInfo.clientType().value());
		connection.setUsername(clientInfo.username());
		connection.setChannelId(channelId);
		connection.setClientAddr(ctx.getClientAddr());
		connection.setServerNode(Broker.getConsistentId());
		connection.setServerAddr(Broker.BROKER_ADDR);
		connection.setServerName(serverName);
		connection.setTransport(Transport.MQTT.name());
		connection.setConnectTime(new Date());

		return connection;
	}

	public final void onInitialized(Function<Void, Boolean> callback) {
		this.initCallback = callback;
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		if (mqttChannelContextWrapper != null) {
			mqttChannelContextWrapper.flushIfNeeded();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		setLoggerMdc(mqttChannelContextWrapper.getClientId(), mqttChannelContextWrapper.getClientAddr(), StreamDirection.DOWN, "---", "EXCEPTION");
		log.error("Client channel throwed exception and forced to close. ctx: {}, cause:", ctx, cause);

		mqttChannelContextWrapper.flushIfNeeded();

		ctx.channel().config().setAutoRead(false);
		ctx.channel().close();
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) {
		if (!ctx.channel().isWritable()) {
			mqttChannelContextWrapper.flushIfNeeded();
		}
		ctx.fireChannelWritabilityChanged();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		setLoggerMdc(mqttChannelContextWrapper.getClientId(), mqttChannelContextWrapper.getClientAddr(), StreamDirection.DOWN, "---", "INACTIVE");
		log.info("Client is inactive");
		isLeaving = true;

		deliveryReader.stop(clientInfo.clientId());

		if (idleTimeoutCheckingTask != null) {
			idleTimeoutCheckingTask.cancel(true);
		}

		scheduler.cancelTasks(ctx.channel().id().asLongText());

		if (sessionExpiryInterval == 0) {
			sessionManager.clean(clientInfo.clientId());
		} else {
			sessionManager.suspend(clientInfo.clientId());
		}

		mqttChannelContextWrapper.getFgTasks().forEach(t -> t.cancel(true));
		if (CHANNEL_CONTEXTS.remove(clientInfo.clientId()) != null) {
			connectionManager.remove(clientInfo.clientId(), ctx.channel().id().asLongText());
		}

		if (lwt != null) {
			publishLwt(ctx);
		}

		ctx.fireChannelInactive();
	}

	private void publishLwt(ChannelHandlerContext ctx) {
		long lastWillId = idGenerator.nextId();

		LastWill lastWill = new LastWill();
		lastWill.setId(lastWillId);
		lastWill.setUsername(clientInfo.username());
		lastWill.setClientId(clientInfo.clientId());
		lastWill.setClientType(ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_CLIENT_TYPE).get().value());
		lastWill.setProductCode(ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_PRODUCT_CODE).get());
		lastWill.setSessionId(this.sessionId);
		lastWill.setTopic(this.lwt.topic());
		lastWill.setQos(this.lwt.qos());
		lastWill.setRetain(this.lwt.retain());
		if (this.lwt.payload() != null) {
			lastWill.setPayload(new String(this.lwt.payload(), StandardCharsets.UTF_8));
		}
		lastWill.setExpiryInterval(Math.min(this.lwt.expiryInterval(), tenantSettings.getMaxMessageExpiryInterval()));
		lastWill.setPayloadFormatIndicator(this.lwt.payloadFormatIndicator());
		lastWill.setContentType(this.lwt.contentType());
		lastWill.setResponseTopic(this.lwt.responseTopic());
		if (this.lwt.correlationData() != null) {
			lastWill.setCorrelationData(new String(this.lwt.correlationData(), StandardCharsets.UTF_8));
		}
		lastWill.setUserProperties(this.lwt.userProperties());
		lastWill.setCreateTime(new Date());

		int delayInterval = Math.max(1, Math.min(this.lwt.delayInterval(), this.sessionExpiryInterval));
		lastWillService.save(lastWillId, lastWill, delayInterval, TimeUnit.SECONDS);
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object packet) {
		assert packet instanceof MqttMessage;

		MqttMessage mqttMessage = (MqttMessage) packet;
		if (mqttMessage.decoderResult().isSuccess()) {
			// TODO record message size
			lastActivateAt = Ticker.systemTicker().read();

			Integer packetId = null;
			if (mqttMessage.variableHeader() instanceof MqttMessageIdVariableHeader varHeader) {
				packetId = varHeader.messageId();
			} else if (mqttMessage.variableHeader() instanceof MqttPublishVariableHeader varHeader) {
				packetId = varHeader.packetId();
			}

			setLoggerMdc(mqttChannelContextWrapper.getClientId(), mqttChannelContextWrapper.getClientAddr(), StreamDirection.UP, packetId != null ? packetId.toString() : "---",
					mqttMessage.fixedHeader().messageType().name());

			TransportPacket thePacket = TransportPacket.EMPTY_PACKET;
			switch (mqttMessage.fixedHeader().messageType()) {
			case CONNECT -> handleDuplicateConnectMessage((MqttConnectMessage) packet);
			case PINGREQ -> thePacket = new PingPacket();
			case SUBSCRIBE -> {
				MqttSubscribeMessage subscribeMessage = (MqttSubscribeMessage) packet;

				ValidateMessageResult subscriptionValidation = validateSubscription(subscribeMessage);
				if (subscriptionValidation.getAction() == Action.CONTINUE) {
					ValidateMessageResult checkResult = checkSubscribePacketIdUsing(subscribeMessage);

					if (checkResult.getAction() == Action.CONTINUE) {
						List<ClientSubscribe> clientSubscribes = buildClientSubscribesFromPacket(subscribeMessage);
						ThingshubMessage stdMessage = new ThingshubMessage();
						stdMessage.setId(packetId.toString());
						stdMessage.setClientId(clientInfo.clientId());
						stdMessage.setVersion("1.0.0");
						stdMessage.setName(mqttMessage.fixedHeader().messageType().name().toLowerCase());
						stdMessage.setTime(DateUtil.now());
						stdMessage.setParams(clientSubscribes);

						thePacket = new SubscribePacket(packetId, stdMessage);
					} else if (checkResult.getAction() == Action.RESPONSE) {
						mqttChannelContextWrapper.writeAndFlush(checkResult.getResponse());
					}
				} else if (subscriptionValidation.getAction() == Action.RESPONSE) {
					mqttChannelContextWrapper.writeAndFlush(subscriptionValidation.getResponse());
				} else if (subscriptionValidation.getAction() == Action.FAREWELL) {
					mqttChannelContextWrapper.goAwayWithFarewell(subscriptionValidation.getResponse(), false);
				} else if (subscriptionValidation.getAction() == Action.JUST_CLOSE) {
					mqttChannelContextWrapper.goAway(false);
				}
			}
			case UNSUBSCRIBE -> {
				MqttUnsubscribeMessage unsubscribeMessage = (MqttUnsubscribeMessage) packet;
				ValidateMessageResult unsubscribeValidation = validateUnsubscribe(unsubscribeMessage);
				if (unsubscribeValidation.getAction() == Action.CONTINUE) {
					ValidateMessageResult checkResult = checkUnsubscribePacketIdUsing(unsubscribeMessage);
					if (checkResult.getAction() == Action.CONTINUE) {
						List<ClientUnsubscribe> clientUnsubscribes = unsubscribeMessage.payload().topics().stream().map(topicFilter -> {
							String[] groupAndTopicFilter = TopicUtils.decode(topicFilter);

							Map<String, Object> props = null;
							MqttProperties mqttProps = unsubscribeMessage.idAndPropertiesVariableHeader().properties();
							if (mqttProps != null && !mqttProps.isEmpty()) {
								props = new HashMap<>();
								for (MqttProperty<?> mqttProp : mqttProps.listAll()) {
									props.put(String.valueOf(mqttProp.propertyId()), JSON.toJSONString(mqttProp.value()));
								}
							}

							return new ClientUnsubscribe(mqttChannelContextWrapper.getClientId(), groupAndTopicFilter[0], groupAndTopicFilter[1], props);
						}).toList();

						ThingshubMessage stdMessage = new ThingshubMessage();
						stdMessage.setId(packetId.toString());
						stdMessage.setClientId(clientInfo.clientId());
						stdMessage.setVersion("1.0.0");
						stdMessage.setName(mqttMessage.fixedHeader().messageType().name().toLowerCase());
						stdMessage.setTime(DateUtil.now());
						stdMessage.setParams(clientUnsubscribes);

						thePacket = new UnsubscribePacket(packetId, stdMessage);
					} else if (checkResult.getAction() == Action.RESPONSE) {
						mqttChannelContextWrapper.writeAndFlush(checkResult.getResponse());
					}
				} else if (unsubscribeValidation.getAction() == Action.FAREWELL) {
					mqttChannelContextWrapper.goAwayWithFarewell(unsubscribeValidation.getResponse(), false);
				} else if (unsubscribeValidation.getAction() == Action.JUST_CLOSE) {
					mqttChannelContextWrapper.goAway(false);
				}
			}
			case PUBLISH -> {
				MqttPublishMessage publishMessage = (MqttPublishMessage) packet;
				if (receivingCount >= tenantSettings.getReceiveMaximum()) {
					handleReceivingMaximumExceeded(publishMessage);
					publishMessage.release();
				}
//				else if (!throttler.pass()) {
//					handlePublishRateExceeded(publishMessage);
//					publishMessage.release();
//				}
				else {
					ValidateMessageResult publishValidation = validatePublish(publishMessage);
					if (publishValidation.getAction() == Action.CONTINUE) {
						String topic = getPublishTopic(publishMessage);

						byte[] payloadInBytes = MessageUtils.readByteBuf(publishMessage.payload());
						int qos = publishMessage.fixedHeader().qosLevel().value();
						boolean isDup = publishMessage.fixedHeader().isDup();
						boolean isRetain = publishMessage.fixedHeader().isRetain();

						Map<String, Object> props = null;
						MqttProperties mqttProps = publishMessage.variableHeader().properties();
						if (mqttProps != null && !mqttProps.isEmpty()) {
							props = new HashMap<>();
							for (MqttProperty<?> mqttProp : mqttProps.listAll()) {
								props.put(String.valueOf(mqttProp.propertyId()), JSON.toJSONString(mqttProp.value()));
							}
						}

						thePacket = new PublishPacket(packetId, topic, qos, isRetain, isDup, props, JSON.parseObject(payloadInBytes, ThingshubMessage.class));
					} else if (publishValidation.getAction() == Action.RESPONSE) {
						mqttChannelContextWrapper.writeAndFlush(publishValidation.getResponse());
					} else if (publishValidation.getAction() == Action.FAREWELL) {
						mqttChannelContextWrapper.goAwayWithFarewell(publishValidation.getResponse(), false);
					} else if (publishValidation.getAction() == Action.JUST_CLOSE) {
						mqttChannelContextWrapper.goAway(false);
					}

					publishMessage.release();
				}
			}
			case PUBACK -> thePacket = new PubAckPacket(packetId);
			case PUBREC -> thePacket = new PubRecPacket(packetId, this.getPubRecReasonCode(mqttMessage));
			case PUBREL -> thePacket = new PubRelPacket(packetId);
			case PUBCOMP -> thePacket = new PubCompPacket(packetId);
			case DISCONNECT -> {
				if (!isDisconnectWithLwt(mqttMessage)) {
					lwt = null;
					lastWillService.cleanLassWillOfSession(this.sessionId);
				}

				Integer sei = getSessionExpiryIntervalInDisconnect(mqttMessage);
				int finalSei = Math.min(sei == null ? sessionExpiryInterval : sei.intValue(), tenantSettings.getMaxSessionExpiryInterval());

				thePacket = new DisconnectPacket(finalSei);
				sessionExpiryInterval = finalSei;
			}
			}

			messageRouter.accept(mqttChannelContextWrapper, thePacket);
		} else {
			ctx.channel().config().setAutoRead(false);
			ctx.executor().schedule(() -> ctx.close(), ThreadLocalRandom.current().nextInt(100, 1000), TimeUnit.MILLISECONDS);
		}
	}

	protected abstract int getProtocolVersion();

	protected abstract void handleDuplicateConnectMessage(MqttConnectMessage connectMessage);

	protected abstract Integer getSessionExpiryIntervalInDisconnect(MqttMessage disconnectMessage);

	protected abstract boolean isDisconnectWithLwt(MqttMessage disconnectMessage);

	protected abstract void handleReceivingMaximumExceeded(MqttPublishMessage publishMessage);

	protected abstract void handlePublishRateExceeded(MqttPublishMessage publishMessage);

	protected abstract ValidateMessageResult validatePublish(MqttPublishMessage publishMessage);

	protected abstract String getPublishTopic(MqttPublishMessage publishMessage);

	protected abstract int getPubRecReasonCode(MqttMessage message);

	protected abstract ValidateMessageResult validateSubscription(MqttSubscribeMessage subscribeMessage);

	protected abstract ValidateMessageResult checkSubscribePacketIdUsing(MqttSubscribeMessage subscribeMessage);

	protected abstract List<ClientSubscribe> buildClientSubscribesFromPacket(MqttSubscribeMessage subscribeMessage);

	protected abstract ValidateMessageResult validateUnsubscribe(MqttUnsubscribeMessage unsubscribeMessage);

	protected abstract ValidateMessageResult checkUnsubscribePacketIdUsing(MqttUnsubscribeMessage unsubscribeMessage);

	protected abstract MqttPublishMessage buildPublishMessageFromDelivery(int packetId, MqttQoS finalMqttQos, Delivery delivery);

}