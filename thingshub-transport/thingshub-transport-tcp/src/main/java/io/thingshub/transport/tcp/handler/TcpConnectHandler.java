package io.thingshub.transport.tcp.handler;

import static io.thingshub.commons.model.MessageResult.BAD_USER_NAME_OR_PASSWORD;
import static io.thingshub.commons.model.MessageResult.CONNECTION_QUOTA_EXCEEDED;
import static io.thingshub.commons.model.MessageResult.CONNECTION_RATE_EXCEEDED;
import static io.thingshub.commons.model.MessageResult.NOT_AUTHORIZED;
import static io.thingshub.commons.model.MessageResult.RESOURCE_QUOTA_EXCEEDED;
import static io.thingshub.commons.model.MessageResult.SERVICE_UNAVAILABLE;
import static io.thingshub.commons.model.MessageResult.SUCCESS;
import static io.thingshub.transport.ChannelContextWrapper.ATTRIBUTE_CLIENT_ID;
import static io.thingshub.transport.ChannelContextWrapper.ATTRIBUTE_CLIENT_TYPE;
import static io.thingshub.transport.ChannelContextWrapper.ATTRIBUTE_CURRENT_SERVER_NAME;
import static io.thingshub.transport.Packet.AUTH_ACK;
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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.slf4j.MDC;

import com.alibaba.fastjson2.JSONObject;

import cn.hutool.core.date.DateUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandler;
import io.thingshub.Broker;
import io.thingshub.commons.model.StreamDirection;
import io.thingshub.commons.model.ThingshubMessage;
import io.thingshub.config.TenantSettings;
import io.thingshub.transport.Packet;
import io.thingshub.transport.TransportPacket;
import io.thingshub.transport.authenticate.AuthRequest;
import io.thingshub.transport.authenticate.AuthRequest.AuthRequestBuilder;
import io.thingshub.transport.authenticate.Authenticator;
import io.thingshub.transport.tcp.TcpChannelContext;
import io.thingshub.transport.tcp.TcpTransportConfig;
import io.thingshub.transport.tcp.codec.MessageTransformDecoder;
import io.thingshub.transport.tcp.codec.MessageTransformEncoder;
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
 * TCP connect handler
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class TcpConnectHandler extends ChannelDuplexHandler {

	public static final Map<String, TcpChannelContext> CHANNEL_CONTEXTS = new ConcurrentHashMap<>();

	private final TcpTransportConfig tcpTransportConfig;

	private final ResourceThrottler resourceThrottler;

	private final Authenticator authenticator;

	private ChannelHandlerContext ctx;

	public TcpConnectHandler(TcpTransportConfig tcpTransportConfig) {
		this.tcpTransportConfig = tcpTransportConfig;

		this.authenticator = Broker.getBean(Authenticator.class);
		this.resourceThrottler = Broker.getBean(ResourceThrottler.class);
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		ctx.fireChannelInactive();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object packet) {
		TransportPacket thePacket = (TransportPacket) packet;
		String deviceSn = ctx.channel().attr(ATTRIBUTE_CLIENT_ID).get();

		if (Packet.AUTH.name().equalsIgnoreCase(thePacket.getPacketName())) {
			String username = null;
			String password = null;

			JSONObject params = (JSONObject) thePacket.getPayload().getParams();
			if (params != null) {
				username = params.getString("username");
				password = params.getString("password");
			}

			AuthRequest authRequest = buildAuthRequest(ctx.channel(), deviceSn, username, password);
			TaskTracker.getInstance().track(CompletableFuture.supplyAsync(() -> authenticator.basicAuth(authRequest), ctx.executor())).thenAccept(authResult -> {
				String tenant = authResult.clientInfo().tenant();

				switch (authResult.resultCode()) {
				case OK -> {
					if (!resourceThrottler.hasResource(tenant, TOTAL_CONNECTIONS)) {
						setLoggerMdc(ctx, StreamDirection.UP, thePacket.getPayload().getId(), thePacket.getPayload().getName());
						log.error("Tenant's resource quota exceeded. tenant: {}, resource type: {}", tenant, TOTAL_CONNECTIONS);

						ThingshubMessage farewell = new ThingshubMessage();
						farewell.setId(thePacket.getPayload().getId());
						farewell.setClientId(deviceSn);
						farewell.setVersion("1.0.0");
						farewell.setName(AUTH_ACK.name().toLowerCase());
						farewell.setTime(DateUtil.now());
						farewell.setCode(CONNECTION_QUOTA_EXCEEDED.code());
						farewell.setMessage(CONNECTION_QUOTA_EXCEEDED.desc());

						closeChannel(farewell);
						return;
					}
					if (!resourceThrottler.hasResource(tenant, TOTAL_SESSION_MEMORY_BYTES)) {
						setLoggerMdc(ctx, StreamDirection.UP, thePacket.getPayload().getId(), thePacket.getPayload().getName());
						log.error("Tenant's resource quota exceeded. tenant: {}, resource type: {}", tenant, TOTAL_SESSION_MEMORY_BYTES);

						ThingshubMessage farewell = new ThingshubMessage();
						farewell.setId(thePacket.getPayload().getId());
						farewell.setClientId(deviceSn);
						farewell.setVersion("1.0.0");
						farewell.setName(AUTH_ACK.name().toLowerCase());
						farewell.setTime(DateUtil.now());
						farewell.setCode(RESOURCE_QUOTA_EXCEEDED.code());
						farewell.setMessage(RESOURCE_QUOTA_EXCEEDED.desc());

						closeChannel(farewell);
						return;
					}
					if (!resourceThrottler.hasResource(tenant, TOTAL_CONNECT_PER_SECOND)) {
						setLoggerMdc(ctx, StreamDirection.UP, thePacket.getPayload().getId(), thePacket.getPayload().getName());
						log.error("Tenant's resource quota exceeded. tenant: {}, resource type: {}", tenant, TOTAL_CONNECT_PER_SECOND);

						ThingshubMessage farewell = new ThingshubMessage();
						farewell.setId(thePacket.getPayload().getId());
						farewell.setClientId(deviceSn);
						farewell.setVersion("1.0.0");
						farewell.setName(AUTH_ACK.name().toLowerCase());
						farewell.setTime(DateUtil.now());
						farewell.setCode(CONNECTION_RATE_EXCEEDED.code());
						farewell.setMessage(CONNECTION_RATE_EXCEEDED.desc());

						closeChannel(farewell);
						return;
					}

					ctx.channel().attr(ATTRIBUTE_CLIENT_ID).set(deviceSn);
					ctx.channel().attr(ATTRIBUTE_CLIENT_TYPE).set(authResult.clientInfo().clientType());
					ctx.channel().attr(ATTRIBUTE_CURRENT_SERVER_NAME).set(tcpTransportConfig.getName());

					Condition slowdownCondition = or(DirectMemPressureCondition.INSTANCE, HeapMemPressureCondition.INSTANCE,
							new InboundResourceCondition(resourceThrottler, authResult.clientInfo().tenant()));
					SlowdownInboundHandler slowdownHandler = new SlowdownInboundHandler(slowdownCondition);
					ctx.pipeline().addFirst(ctx.executor(), SlowdownInboundHandler.class.getSimpleName(), slowdownHandler);

					MessageTransformDecoder tcpTransformDecoder = Broker.getBean(MessageTransformDecoder.class);
					ctx.pipeline().addBefore(ctx.executor(), this.getClass().getSimpleName(), MessageTransformDecoder.class.getSimpleName(), tcpTransformDecoder);
					MessageTransformEncoder tcpTransformEncoder = Broker.getBean(MessageTransformEncoder.class);
					ctx.pipeline().addBefore(ctx.executor(), this.getClass().getSimpleName(), MessageTransformEncoder.class.getSimpleName(), tcpTransformEncoder);

					int keepAlive = tcpTransportConfig.getKeepAlive();
					keepAlive = Math.max(5, keepAlive);
					keepAlive = Math.min(keepAlive, 2 * 60 * 60);

					TenantSettings tenantSettings = TenantSettings.builder().tenant(authResult.clientInfo().tenant()).build();
					TcpSessionHandler sessionHandler = new TcpSessionHandler(tenantSettings, authResult.clientInfo(), keepAlive);
					sessionHandler.onInitialized(v -> {
						setLoggerMdc(ctx, StreamDirection.UP, thePacket.getPayload().getId(), thePacket.getPayload().getName());
						log.info("Client connected");

						final AtomicReference<Boolean> connAckedRef = new AtomicReference<>(Boolean.FALSE);

						ThingshubMessage theReply = new ThingshubMessage();
						theReply.setId(thePacket.getPayload().getId());
						theReply.setClientId(deviceSn);
						theReply.setVersion("1.0.0");
						theReply.setName(AUTH_ACK.name().toLowerCase());
						theReply.setTime(DateUtil.now());
						theReply.setCode(SUCCESS.code());
						theReply.setMessage(SUCCESS.desc());

						ctx.writeAndFlush(theReply).addListener(new ChannelFutureListener() {

							@Override
							public void operationComplete(ChannelFuture future) throws Exception {
								if (!future.isSuccess()) {
									setLoggerMdc(ctx, StreamDirection.UP, thePacket.getPayload().getId(), thePacket.getPayload().getName());
									log.error("Server has authenticated device [{}] but failed to send the reply to device");
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
					ctx.pipeline().replace(this, TcpSessionHandler.class.getSimpleName(), sessionHandler);
				}
				case BAD_PASS -> {
					ThingshubMessage badPassFarewell = new ThingshubMessage();
					badPassFarewell.setId(thePacket.getPayload().getId());
					badPassFarewell.setClientId(deviceSn);
					badPassFarewell.setVersion("1.0.0");
					badPassFarewell.setName(AUTH_ACK.name().toLowerCase());
					badPassFarewell.setTime(DateUtil.now());
					badPassFarewell.setCode(BAD_USER_NAME_OR_PASSWORD.code());
					badPassFarewell.setMessage(BAD_USER_NAME_OR_PASSWORD.desc());

					closeChannel(badPassFarewell);
				}
				case NOT_AUTHORIZED -> {
					ThingshubMessage unauthorizedFarewell = new ThingshubMessage();
					unauthorizedFarewell.setId(thePacket.getPayload().getId());
					unauthorizedFarewell.setClientId(deviceSn);
					unauthorizedFarewell.setVersion("1.0.0");
					unauthorizedFarewell.setName(AUTH_ACK.name().toLowerCase());
					unauthorizedFarewell.setTime(DateUtil.now());
					unauthorizedFarewell.setCode(NOT_AUTHORIZED.code());
					unauthorizedFarewell.setMessage(NOT_AUTHORIZED.desc());

					closeChannel(unauthorizedFarewell);
				}
				default -> {
					setLoggerMdc(ctx, StreamDirection.UP, thePacket.getPayload().getId(), thePacket.getPayload().getName());
					log.error("Unexpected error from auth manager, auth result: {}", authResult.resultCode().name());

					ThingshubMessage farewell = new ThingshubMessage();
					farewell.setId(thePacket.getPayload().getId());
					farewell.setClientId(deviceSn);
					farewell.setVersion("1.0.0");
					farewell.setName(AUTH_ACK.name().toLowerCase());
					farewell.setTime(DateUtil.now());
					farewell.setCode(SERVICE_UNAVAILABLE.code());
					farewell.setMessage(SERVICE_UNAVAILABLE.desc());

					closeChannel(farewell);
				}
				}
			});
		} else {
			setLoggerMdc(ctx, StreamDirection.UP, thePacket.getPayload().getId(), thePacket.getPayload().getName());
			log.error("Before establishing a connection, device must be authenticated. Device sn: {},  message name: {}", deviceSn, thePacket.getPayload().getName());
			closeChannel(null);
		}
	}

	private void setLoggerMdc(ChannelHandlerContext ctx, StreamDirection streamDirection, String packetId, String msgName) {
		String deviceSn = ctx.channel().attr(ATTRIBUTE_CLIENT_ID).get();
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

	private AuthRequest buildAuthRequest(Channel channel, String clientId, String username, String password) {
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

		builder.clientId(clientId).username(username).password(password);

		InetSocketAddress remoteAddr = (InetSocketAddress) channel.remoteAddress();
		if (remoteAddr != null) {
			builder.remotePort(remoteAddr.getPort());
			InetAddress ip = remoteAddr.getAddress();
			if (remoteAddr.getAddress() != null) {
				builder.remoteAddr(ip.getHostAddress());
			}
		}

		return builder.build();
	}

	private void closeChannel(ThingshubMessage farewell) {
		assert ctx.executor().inEventLoop();

		if (!ctx.channel().isActive()) {
			return;
		}

		ctx.executor().schedule(() -> {
			if (farewell != null) {
				ctx.writeAndFlush(farewell).addListener(ChannelFutureListener.CLOSE);
			} else {
				ctx.channel().close();
			}
		}, ThreadLocalRandom.current().nextInt(100, 1000), TimeUnit.MILLISECONDS);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		log.warn("ctx: {}, cause:", ctx, cause);
		ctx.channel().close();
	}

}