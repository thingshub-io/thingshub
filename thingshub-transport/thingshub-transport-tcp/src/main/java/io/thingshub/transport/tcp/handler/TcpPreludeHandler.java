package io.thingshub.transport.tcp.handler;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import io.thingshub.Broker;
import io.thingshub.commons.model.StreamDirection;
import io.thingshub.commons.model.ThingshubMessage;
import io.thingshub.entity.Device;
import io.thingshub.entity.ScriptInfo;
import io.thingshub.script.ScriptEngine;
import io.thingshub.script.ScriptEngineFactory;
import io.thingshub.service.DeviceService;
import io.thingshub.service.ScriptService;
import io.thingshub.transport.ChannelContextWrapper;
import io.thingshub.transport.Packet;
import io.thingshub.transport.TransportPacket;
import io.thingshub.transport.tcp.TcpTransportConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Prepare for a new TCP connection
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class TcpPreludeHandler extends ChannelDuplexHandler {

	private final TcpTransportConfig tcpTransportConfig;

	private ChannelHandlerContext ctx;

	private ScheduledFuture<?> timeoutCloseTask;

	private ScheduledFuture<?> closeConnectionTask;

	public TcpPreludeHandler(TcpTransportConfig tcpServerConfig) {
		this.tcpTransportConfig = tcpServerConfig;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		this.ctx = ctx;

		timeoutCloseTask = ctx.executor().schedule(() -> {
			ctx.channel().close();
		}, tcpTransportConfig.getConnectTimeout(), TimeUnit.SECONDS);
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		if (timeoutCloseTask != null) {
			timeoutCloseTask.cancel(true);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		if (timeoutCloseTask != null) {
			timeoutCloseTask.cancel(true);
		}
		ctx.fireChannelInactive();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		ctx.channel().config().setAutoRead(false);
		timeoutCloseTask.cancel(true);

		if (msg instanceof ByteBuf buf) {
			try {
				if (buf.readableBytes() <= 0) {
					return;
				}

				final byte[] msgBytes = new byte[buf.readableBytes()];
				buf.readBytes(msgBytes);

				ScriptInfo theScript = Broker.getBean(ScriptService.class).getScript(tcpTransportConfig.getName());
				if (theScript == null) {
					setLoggerMdc(ctx, null);
					log.error("Failed to parse first packet. Error: no prehandle script specified for server");
					closeChannel(null);
				} else {
					ScriptEngine scriptEngine = Broker.getBean(ScriptEngineFactory.class).getScriptEngine(theScript.getLang());
					String strResult = scriptEngine.invoke(theScript.getName(), String.class, "preHandle", msgBytes);
					if (strResult != null && !strResult.isBlank()) {
						JSONObject objResult = JSON.parseObject(strResult);
						String deviceSn = objResult.getString("sn");
//					int thePacketId = objResult.getIntValue("packetId");
						int thePacketType = objResult.getIntValue("packetType");
//					String thePacketName = objResult.getString("packetName");

						Device device = Broker.getBean(DeviceService.class).getBySn(deviceSn);
						if (device != null) {
							String strTransportPacket = scriptEngine.invoke(device.getProductCode(), String.class, "decode", deviceSn, msgBytes);
							if (strTransportPacket != null) {
								ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_CLIENT_ID).set(deviceSn);
								ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_PRODUCT_CODE).set(device.getProductCode());
								ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_SCRIPT_LANG).set(theScript.getLang());

								TransportPacket thePacket = JSON.parseObject(strTransportPacket, TransportPacket.class);

								switch (Packet.of(thePacketType)) {
								case REGISTER -> {
									// TODO register device and get auth code or secret
								}
								case AUTH -> {
									ctx.pipeline().addAfter(ctx.executor(), this.getClass().getSimpleName(), TcpConnectHandler.class.getSimpleName(),
											new TcpConnectHandler(tcpTransportConfig));
									ctx.fireChannelRead(thePacket);
									ctx.pipeline().remove(this.getClass().getSimpleName());
								}
								default -> {
									setLoggerMdc(ctx, deviceSn);
									log.error("First packet must be a register packet or an auth packet. raw data: {}", new String(msgBytes, StandardCharsets.UTF_8));
									closeChannel(null);
								}
								}
							} else {
								setLoggerMdc(ctx, deviceSn);
								log.error("Failed to parse first packet. Raw data: {}", new String(msgBytes, StandardCharsets.UTF_8));
								closeChannel(null);
							}
						} else {
							setLoggerMdc(ctx, deviceSn);
							log.error("Invalid device sn in packet. Device sn: {}, raw data: {}", deviceSn, new String(msgBytes, StandardCharsets.UTF_8));
							closeChannel(null);
						}
					} else {
						setLoggerMdc(ctx, null);
						log.error("Failed to parse device sn from first packet. Raw data: {}", new String(msgBytes, StandardCharsets.UTF_8));
						closeChannel(null);
					}
				}
			} catch (Exception e) {
				setLoggerMdc(ctx, null);
				log.error("Failed to parse first packet. Error: ", e);
				closeChannel(null);
			} finally {
				ReferenceCountUtil.release(buf);
			}
		} else {
			closeChannel(null);
		}
	}

	private void setLoggerMdc(ChannelHandlerContext ctx, String deviceSn) {
		InetSocketAddress remoteAddr = (InetSocketAddress) ctx.channel().remoteAddress();
		String clientAddr = null;
		if (remoteAddr != null) {
			InetAddress ip = remoteAddr.getAddress();
			if (remoteAddr.getAddress() != null) {
				clientAddr = ip.getHostAddress();
			}
		}

		MDC.put("clientId", deviceSn != null ? deviceSn : "---");
		MDC.put("clientAddr", clientAddr);
		MDC.put("protocol", "TCP");
		MDC.put("streamDirection", StreamDirection.UP.name());
		MDC.put("serverAddr", Broker.BROKER_ADDR);
		MDC.put("packetId", "---");
		MDC.put("msgName", "CONNECTING");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		log.error("", cause);
		ctx.channel().close();
	}

	private void closeChannel(ThingshubMessage farewell) {
		assert ctx.executor().inEventLoop();

		if (timeoutCloseTask != null) {
			timeoutCloseTask.cancel(true);
		}

		if (!ctx.channel().isActive()) {
			return;
		}

		assert closeConnectionTask == null;
		closeConnectionTask = ctx.executor().schedule(() -> {
			if (farewell != null) {
				ctx.writeAndFlush(farewell).addListener(ChannelFutureListener.CLOSE);
			} else {
				ctx.channel().close();
			}
		}, ThreadLocalRandom.current().nextInt(100, 3000), TimeUnit.MILLISECONDS);
	}

}