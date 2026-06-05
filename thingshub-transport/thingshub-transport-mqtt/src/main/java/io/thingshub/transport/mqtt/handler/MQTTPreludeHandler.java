package io.thingshub.transport.mqtt.handler;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_CLIENT_IDENTIFIER_NOT_VALID;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_MALFORMED_PACKET;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_PACKET_TOO_LARGE;
import static io.netty.handler.codec.mqtt.MqttMessageType.CONNECT;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.REASON_STRING;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.slf4j.MDC;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttIdentifierRejectedException;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttProperties.StringProperty;
import io.netty.handler.codec.mqtt.MqttUnacceptableProtocolVersionException;
import io.thingshub.Broker;
import io.thingshub.commons.StreamDirection;
import io.thingshub.transport.mqtt.MqttTransportConfig;
import io.thingshub.transport.mqtt.handler.v3.MQTT3ConnectHandler;
import io.thingshub.transport.mqtt.handler.v5.MQTT5ConnectHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Prepare for a new MQTT connection
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class MQTTPreludeHandler extends ChannelDuplexHandler {

	private MqttTransportConfig mqttTransportConfig;

	private ChannelHandlerContext ctx;

	private ScheduledFuture<?> connectionTimeoutTask;

	private ScheduledFuture<?> connectionClosingTask;

	public MQTTPreludeHandler(MqttTransportConfig mqttTransportConfig) {
		this.mqttTransportConfig = mqttTransportConfig;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		this.ctx = ctx;

		connectionTimeoutTask = ctx.executor().schedule(() -> {
			ctx.channel().close();
		}, mqttTransportConfig.getConnectTimeout(), TimeUnit.SECONDS);
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		if (connectionTimeoutTask != null) {
			connectionTimeoutTask.cancel(true);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		if (connectionTimeoutTask != null) {
			connectionTimeoutTask.cancel(true);
		}
		ctx.fireChannelInactive();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		assert msg instanceof MqttMessage;

		ctx.channel().config().setAutoRead(false);
		connectionTimeoutTask.cancel(true);

		MqttMessage message = (MqttMessage) msg;
		if (!message.decoderResult().isSuccess()) {
			Throwable cause = message.decoderResult().cause();
			if (cause instanceof MqttUnacceptableProtocolVersionException) {
				setLoggerMdc(ctx);
				log.error("Protocol error: unaccepted protocol version", cause);
				closeChannelInRandomDelay(null);
				return;
			}
			if (message.fixedHeader() != null && message.fixedHeader().messageType() != CONNECT) {
				setLoggerMdc(ctx);
				log.error("Protocol error: the first Packet must be a CONNECT Packet");
				closeChannelInRandomDelay(null);
				return;
			}

			Object varHeader = message.variableHeader();
			if (varHeader instanceof MqttConnectVariableHeader connVarHeader) {
				switch (connVarHeader.version()) {
				case 3, 4 -> {
					if (cause instanceof TooLongFrameException) {
						setLoggerMdc(ctx);
						log.error("Protocol error: too large packet", cause);
						closeChannelInRandomDelay(null);
					} else if (cause instanceof MqttIdentifierRejectedException) {
						setLoggerMdc(ctx);
						log.error("Protocol error: identifier rejected", cause);

						closeChannelInRandomDelay(MqttMessageBuilders.connAck().returnCode(CONNECTION_REFUSED_IDENTIFIER_REJECTED).build());
					} else {
						setLoggerMdc(ctx);
						log.error("", cause);
						closeChannelInRandomDelay(null);
					}
				}
				case 5 -> {
					MqttProperties props = new MqttProperties();
					if (cause.getMessage() != null) {
						props.add(new StringProperty(REASON_STRING.value(), cause.getMessage()));
					}

					if (cause instanceof TooLongFrameException) {
						setLoggerMdc(ctx);
						log.error("Protocol error: too large packet", cause);
						closeChannelInRandomDelay(MqttMessageBuilders.connAck().properties(props).returnCode(CONNECTION_REFUSED_PACKET_TOO_LARGE).build());
					} else if (cause instanceof MqttIdentifierRejectedException) {
						setLoggerMdc(ctx);
						log.error("Protocol error: client identifier not valid", cause);
						closeChannelInRandomDelay(MqttMessageBuilders.connAck().properties(props).returnCode(CONNECTION_REFUSED_CLIENT_IDENTIFIER_NOT_VALID).build());
					} else {
						setLoggerMdc(ctx);
						log.error("Protocol error: malformed packet", cause);
						closeChannelInRandomDelay(MqttMessageBuilders.connAck().properties(props).returnCode(CONNECTION_REFUSED_MALFORMED_PACKET).build());
					}
				}
				}

				return;
			} else {
				setLoggerMdc(ctx);
				log.error("Protocol error: ", cause);
				closeChannelInRandomDelay(null);// other
				return;
			}
		} else if (!(message instanceof MqttConnectMessage)) {
			setLoggerMdc(ctx);
			log.error("Protocol error: the first Packet must be a CONNECT Packet");

			closeChannelInRandomDelay(null);
			return;
		}

		MqttConnectMessage connectMessage = (MqttConnectMessage) message;
		switch (connectMessage.variableHeader().version()) {
		case 3, 4 -> {
			ctx.pipeline().addAfter(ctx.executor(), this.getClass().getSimpleName(), MQTT3ConnectHandler.class.getSimpleName(), new MQTT3ConnectHandler(mqttTransportConfig));
			ctx.fireChannelRead(connectMessage);
			ctx.pipeline().remove(this.getClass().getSimpleName());
		}
		case 5 -> {
			ctx.pipeline().addAfter(ctx.executor(), this.getClass().getSimpleName(), MQTT5ConnectHandler.class.getSimpleName(), new MQTT5ConnectHandler(mqttTransportConfig));
			ctx.fireChannelRead(connectMessage);
			ctx.pipeline().remove(this.getClass().getSimpleName());
		}
		default -> {
			setLoggerMdc(ctx);
			log.warn("Unsupported protocol version: {}", connectMessage.variableHeader().version());
		}
		}
	}

	private void setLoggerMdc(ChannelHandlerContext ctx) {
		InetSocketAddress remoteAddr = (InetSocketAddress) ctx.channel().remoteAddress();
		String clientAddr = null;
		if (remoteAddr != null) {
			InetAddress ip = remoteAddr.getAddress();
			if (remoteAddr.getAddress() != null) {
				clientAddr = ip.getHostAddress();
			}
		}

		MDC.put("clientId", "---");
		MDC.put("clientAddr", clientAddr);
		MDC.put("protocol", "MQTT");
		MDC.put("streamDirection", StreamDirection.UP.name());
		MDC.put("serverAddr", Broker.BROKER_ADDR);
		MDC.put("packetId", "---");
		MDC.put("msgName", "CONNECTING");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		setLoggerMdc(ctx);
		log.error("Client channel throwed exception and forced to close. ctx: {}, cause:", ctx, cause);

		ctx.channel().close();
	}

	private void closeChannelInRandomDelay(@Nullable MqttMessage farewell) {
		if (connectionTimeoutTask != null) {
			connectionTimeoutTask.cancel(true);
		}

		if (!ctx.channel().isActive()) {
			return;
		}

		assert connectionClosingTask == null;
		connectionClosingTask = ctx.executor().schedule(() -> {
			if (farewell != null) {
				ctx.writeAndFlush(farewell).addListener(ChannelFutureListener.CLOSE);
			} else {
				ctx.channel().close();
			}
		}, ThreadLocalRandom.current().nextInt(3000), TimeUnit.MILLISECONDS);
	}

}