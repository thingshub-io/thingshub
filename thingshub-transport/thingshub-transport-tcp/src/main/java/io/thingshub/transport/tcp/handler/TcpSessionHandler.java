package io.thingshub.transport.tcp.handler;

import static io.thingshub.commons.model.ThingshubConstants.THING_TOPIC_SERVICE_FUNCTION_CALL;
import static io.thingshub.commons.model.ThingshubConstants.THING_TOPIC_SERVICE_REQUEST_REPLY;
import static io.thingshub.transport.ChannelContextWrapper.ATTRIBUTE_CURRENT_SERVER_NAME;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Date;
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
import com.google.common.collect.Maps;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.thingshub.Broker;
import io.thingshub.commons.model.MessageType;
import io.thingshub.commons.model.StreamDirection;
import io.thingshub.commons.model.ThingshubMessage;
import io.thingshub.config.TenantSettings;
import io.thingshub.entity.Connection;
import io.thingshub.entity.MessageDefinition;
import io.thingshub.schedule.Action;
import io.thingshub.schedule.Scheduler;
import io.thingshub.service.MessageDefinitionService;
import io.thingshub.service.base.IdGenerator;
import io.thingshub.service.model.ClientInfo;
import io.thingshub.service.model.ClientType;
import io.thingshub.service.model.Delivery;
import io.thingshub.subscribe.ClientSubscribe;
import io.thingshub.subscribe.SubscriptionManager;
import io.thingshub.transport.ChannelContextWrapper;
import io.thingshub.transport.ConnectionManager;
import io.thingshub.transport.MessageRouter;
import io.thingshub.transport.Packet;
import io.thingshub.transport.SessionManager;
import io.thingshub.transport.Transport;
import io.thingshub.transport.TransportPacket;
import io.thingshub.transport.tcp.TcpChannelContext;
import io.thingshub.transport.tcp.packet.PublishPacket;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * TCP session handler
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class TcpSessionHandler extends ChannelDuplexHandler {

	public static final Map<String, TcpChannelContext> CHANNEL_CONTEXTS = new ConcurrentHashMap<>();

	private final TenantSettings tenantSettings;

	private final ClientInfo clientInfo;

	private final int keepAlive;

	private final long idleTimeout;

	private final ConnectionManager connectionManager;

	private final SessionManager sessionManager;

	private final MessageRouter messageRouter;

	private final MessageDefinitionService messageDefinitionService;

	private final SubscriptionManager subscriptionManager;

	private final IdGenerator idGenerator;

	private final Scheduler scheduler;

	private TcpChannelContext tcpChannelContext;

	private long lastActiveAt;

	private ScheduledFuture<?> idleTimeoutTask;

	private Function<Void, Boolean> initCallback;

	private static void setLoggerMdc(ChannelHandlerContext ctx, StreamDirection streamDirection, String packetId, String msgName) {
		String deviceSn = ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_CLIENT_ID).get();
		InetSocketAddress remoteAddr = (InetSocketAddress) ctx.channel().remoteAddress();
		String clientAddr = null;
		if (remoteAddr != null) {
			InetAddress ip = remoteAddr.getAddress();
			if (remoteAddr.getAddress() != null) {
				clientAddr = ip.getHostAddress();
			}
		}

		MDC.put("clientId", deviceSn);
		MDC.put("clientAddr", clientAddr);
		MDC.put("protocol", "TCP");
		MDC.put("streamDirection", streamDirection.name());
		MDC.put("serverAddr", Broker.BROKER_ADDR);
		MDC.put("packetId", packetId);
		MDC.put("msgName", msgName);
	}

	private void onKick(String clientId, String channelId) {
		TcpChannelContext ctx = CHANNEL_CONTEXTS.get(clientId);
		if (ctx == null) {
			return;
		}

		assert ctx.executor().inEventLoop();

		if (!ctx.channel().isActive()) {
			return;
		}

		setLoggerMdc(ctx, StreamDirection.DOWN, "---", "KICK");
		log.info("Client with channel id [{}] in node [{}] is kicked", ctx.channel().id().asLongText(), Broker.BROKER_ADDR);

		ctx.goAway(true);
	}

	private static void onDeliver(Delivery delivery) {
		TcpChannelContext ctx = TcpSessionHandler.CHANNEL_CONTEXTS.get(delivery.receiverId());
		if (ctx == null) {
			return;
		}

		if (!ctx.channel().isActive()) {
			ThingshubMessage thingshubMessage = JSON.parseObject(delivery.payload(), ThingshubMessage.class);
			setLoggerMdc(ctx, StreamDirection.DOWN, thingshubMessage.getId(), thingshubMessage.getName());
			log.error("Channel is inactive and sending packet is dropped");

			return;
		}
		if (!ctx.channel().isWritable()) {
			ThingshubMessage thingshubMessage = JSON.parseObject(delivery.payload(), ThingshubMessage.class);
			setLoggerMdc(ctx, StreamDirection.DOWN, thingshubMessage.getId(), thingshubMessage.getName());
			log.error("Channel is not writable and sending packet is dropped");

			return;
		}

		ctx.addFgTask(CompletableFuture.runAsync(() -> {
			ThingshubMessage thingshubMessage = JSON.parseObject(delivery.payload(), ThingshubMessage.class);
			String channelId = ctx.channel().id().asLongText();
			String taskId = thingshubMessage.getId();
			int resendTimes = ctx.getTenantSettings().getMaxResendTimes();
			Action retryAction = () -> {
				ctx.writeAndFlush(thingshubMessage).addListener(new ChannelFutureListener() {

					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						if (future.isSuccess()) {
							setLoggerMdc(ctx, StreamDirection.DOWN, thingshubMessage.getId(), thingshubMessage.getName());
							log.info("Resend packet successfully");

							Broker.getBean(Scheduler.class).cancelTask(channelId, taskId);
						} else {
							setLoggerMdc(ctx, StreamDirection.DOWN, thingshubMessage.getId(), thingshubMessage.getName());
							log.error("Resend packet error");
							if (future.cause() != null) {
								log.error("", future.cause());
							}
						}
					}

				});
			};

			ctx.writeAndFlush(thingshubMessage).addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (!future.isSuccess()) {
						setLoggerMdc(ctx, StreamDirection.DOWN, thingshubMessage.getId(), thingshubMessage.getName());
						log.error("Send packet error and will delay resending packet");
						if (future.cause() != null) {
							log.error("", future.cause());
						}

						Broker.getBean(Scheduler.class).addTask(channelId, taskId, retryAction, 2000L, resendTimes);
					} else {
						Broker.getBean(Scheduler.class).cancelTask(channelId, taskId);
					}
				}

			});
		}, ctx.executor()));
	}

	public TcpSessionHandler(TenantSettings tenantSettings, ClientInfo clientInfo, int keepAlive) {
		this.tenantSettings = tenantSettings;
		this.clientInfo = clientInfo;
		this.keepAlive = keepAlive;
		this.idleTimeout = Duration.ofMillis(keepAlive * 1500L).toNanos();

		this.connectionManager = Broker.getBean(ConnectionManager.class);
		this.sessionManager = Broker.getBean(SessionManager.class);
		this.messageRouter = Broker.getBean(MessageRouter.class);
		this.messageDefinitionService = Broker.getBean(MessageDefinitionService.class);
		this.subscriptionManager = Broker.getBean(SubscriptionManager.class);
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

		InetSocketAddress remoteAddr = (InetSocketAddress) ctx.channel().remoteAddress();
		String clientAddr = null;
		if (remoteAddr != null) {
			InetAddress ip = remoteAddr.getAddress();
			if (remoteAddr.getAddress() != null) {
				clientAddr = ip.getHostAddress();
			}
		}
		this.tcpChannelContext = new TcpChannelContext(ctx, tenantSettings, clientInfo.username(), clientInfo.clientId(), clientAddr, keepAlive);
		CHANNEL_CONTEXTS.put(clientInfo.clientId(), this.tcpChannelContext);

		this.idleTimeoutTask = ctx.executor().scheduleAtFixedRate(this::checkIdle, idleTimeout, idleTimeout, TimeUnit.NANOSECONDS);
		this.lastActiveAt = Ticker.systemTicker().read();

		connectionManager.add(buildConnectionInfo(this.tcpChannelContext));
		sessionManager.create(clientInfo.clientId(), Broker.getConsistentId(), 0);

		String productCode = ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_PRODUCT_CODE).get();
		List<MessageDefinition> downwardMessages = messageDefinitionService.getDownwardMessages(productCode);
		if (downwardMessages != null) {
			downwardMessages.forEach(spec -> {
				String topicFilter = buildTopicFilterByDownwardMessage(productCode, spec);

				if (topicFilter != null) {
					ClientType clientType = ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_CLIENT_TYPE).get();
					ClientSubscribe clientSubscribe = new ClientSubscribe(clientInfo.clientId(), clientType, null, topicFilter, Maps.newHashMap());
					subscriptionManager.subscribe(clientSubscribe);
				}
			});
		}

		if (this.initCallback != null) {
			Boolean isCallbackSuccess = this.initCallback.apply(null);
			if (isCallbackSuccess) {
				ctx.channel().config().setAutoRead(true);
				ctx.read();
			}
		}
	}

	private Connection buildConnectionInfo(TcpChannelContext tcpChannelContext) {
		String channelId = tcpChannelContext.channel().id().asLongText();
		String serverName = tcpChannelContext.channel().attr(ATTRIBUTE_CURRENT_SERVER_NAME).get();

		Connection connection = new Connection();
		connection.setTenantId(clientInfo.tenant());
		connection.setClientId(clientInfo.clientId());
		connection.setClientType(clientInfo.clientType().value());
		connection.setUsername(clientInfo.username());
		connection.setChannelId(channelId);
		connection.setClientAddr(tcpChannelContext.getClientAddr());
		connection.setServerNode(Broker.getConsistentId());
		connection.setServerAddr(Broker.BROKER_ADDR);
		connection.setServerName(serverName);
		connection.setTransport(Transport.TCP.name());
		connection.setConnectTime(new Date());

		return connection;
	}

	private String buildTopicFilterByDownwardMessage(String productCode, MessageDefinition downwardMessage) {
		return switch (MessageType.valueOf(downwardMessage.getType())) {
		case SERVICE_FUNCTION_CALL -> String.format(THING_TOPIC_SERVICE_FUNCTION_CALL, productCode, clientInfo.clientId(), downwardMessage.getName());
		case SERVICE_REQUEST_REPLY -> String.format(THING_TOPIC_SERVICE_REQUEST_REPLY, productCode, clientInfo.clientId(), downwardMessage.getName());
		default -> null;
		};
	}

	private void checkIdle() {
		if ((Ticker.systemTicker().read() - lastActiveAt) > idleTimeout) {
			setLoggerMdc(tcpChannelContext, StreamDirection.DOWN, "---", "IDLE_TIMEOUT");
			log.warn("Client idle timeout and forced to close", clientInfo.clientId());

			idleTimeoutTask.cancel(true);
			tcpChannelContext.channel().config().setAutoRead(false);
			tcpChannelContext.close();
		}
	}

	public final void onInitialized(Function<Void, Boolean> callback) {
		this.initCallback = callback;
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		if (tcpChannelContext != null) {
			tcpChannelContext.flushIfNeeded();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		setLoggerMdc(ctx, StreamDirection.DOWN, "---", "EXCEPTION");
		log.error("Client channel throwed exception and forced to close. ctx: {}, cause:", ctx, cause);

		tcpChannelContext.flushIfNeeded();

		ctx.channel().config().setAutoRead(false);
		ctx.channel().close();
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) {
		if (!ctx.channel().isWritable()) {
			tcpChannelContext.flushIfNeeded();
		}
		ctx.fireChannelWritabilityChanged();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		setLoggerMdc(ctx, StreamDirection.DOWN, "---", "INACTIVE");
		log.info("Client is inactive");

		if (idleTimeoutTask != null) {
			idleTimeoutTask.cancel(true);
		}

		scheduler.cancelTasks(ctx.channel().id().asLongText());
		sessionManager.clean(clientInfo.clientId());

		tcpChannelContext.getFgTasks().forEach(t -> t.cancel(true));
		connectionManager.remove(clientInfo.clientId(), ctx.channel().id().asLongText());
		CHANNEL_CONTEXTS.remove(clientInfo.clientId());

		ctx.fireChannelInactive();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object packet) {
		assert packet instanceof TransportPacket;

		TransportPacket thePacket = (TransportPacket) packet;
		lastActiveAt = Ticker.systemTicker().read();

		setLoggerMdc(ctx, StreamDirection.UP, thePacket.getPayload().getId(), thePacket.getPayload().getName());

		if (Packet.AUTH.name().equalsIgnoreCase(thePacket.getPacketName())) {
			log.error("Can't send duplicate authenticate packet after completing authentication");

			ctx.channel().config().setAutoRead(false);
			ctx.executor().schedule(() -> ctx.close(), ThreadLocalRandom.current().nextInt(100, 1000), TimeUnit.MILLISECONDS);

			thePacket = TransportPacket.EMPTY_PACKET;
		} else {
			thePacket = new PublishPacket(thePacket.getPacketId(), thePacket.getPayload());
		}

		messageRouter.accept(tcpChannelContext, thePacket);
	}

}