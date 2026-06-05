package io.thingshub.transport.tcp.codec;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;

import com.alibaba.fastjson2.JSON;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import io.thingshub.Broker;
import io.thingshub.commons.StreamDirection;
import io.thingshub.ioc.Component;
import io.thingshub.script.ScriptEngine;
import io.thingshub.script.ScriptEngineFactory;
import io.thingshub.script.ScriptException;
import io.thingshub.transport.ChannelContextWrapper;
import io.thingshub.transport.TransportPacket;
import io.thingshub.transport.tcp.TcpChannelContext;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Transform custom TCP payload into standard message model payload
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
@ChannelHandler.Sharable
@Component
public class MessageTransformDecoder extends ChannelInboundHandlerAdapter {

	private final ScriptEngineFactory scriptEngineFactory;

	@Inject
	public MessageTransformDecoder(ScriptEngineFactory scriptEngineFactory) {
		this.scriptEngineFactory = scriptEngineFactory;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof ByteBuf buf) {
			try {
				if (buf.readableBytes() <= 0) {
					return;
				}

				String deviceSn = ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_CLIENT_ID).get();
				String productCode = ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_PRODUCT_CODE).get();
				String scriptLang = ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_SCRIPT_LANG).get();
				ScriptEngine scriptEngine = scriptEngineFactory.getScriptEngine(scriptLang);
				final byte[] msgBytes = new byte[buf.readableBytes()];
				buf.readBytes(msgBytes);

				try {
					String strTransportPacket = scriptEngine.invoke(productCode, String.class, "decode", deviceSn, msgBytes);
					TransportPacket thePacket = JSON.parseObject(strTransportPacket, TransportPacket.class);
					TcpChannelContext.bufferIncoming(ctx.channel().id().asLongText().concat("_" + thePacket.getPacketId()).concat("_payload"), msgBytes);

					if (log.isDebugEnabled()) {
						setLoggerMdc(ctx, thePacket.getPayload().getName(), thePacket.getPayload().getId());
						log.debug("TcpTransformDecoder invoke function {} to transform message, custom message: {}, standard message: {}", "decode",
								new String(msgBytes, StandardCharsets.UTF_8), thePacket.getPayload());
					}

					ctx.fireChannelRead(thePacket);
				} catch (ScriptException se) {
					setLoggerMdc(ctx, null, null);
					log.error("", se);
				}
			} catch (Exception e) {
				setLoggerMdc(ctx, null, null);
				log.error("", e);

				ctx.executor().schedule(() -> ctx.channel().close(), ThreadLocalRandom.current().nextInt(100, 3000), TimeUnit.MILLISECONDS);
			} finally {
				ReferenceCountUtil.release(buf);
			}
		} else {
			ctx.executor().schedule(() -> ctx.channel().close(), ThreadLocalRandom.current().nextInt(100, 3000), TimeUnit.MILLISECONDS);
		}
	}

	private void setLoggerMdc(ChannelHandlerContext ctx, String msgName, String packetId) {
		InetSocketAddress remoteAddr = (InetSocketAddress) ctx.channel().remoteAddress();
		String clientAddr = null;
		if (remoteAddr != null) {
			InetAddress ip = remoteAddr.getAddress();
			if (remoteAddr.getAddress() != null) {
				clientAddr = ip.getHostAddress();
			}
		}

		MDC.put("clientId", ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_CLIENT_ID).get());
		MDC.put("clientAddr", clientAddr);
		MDC.put("protocol", "TCP");
		MDC.put("streamDirection", StreamDirection.UP.name());
		MDC.put("serverAddr", Broker.BROKER_ADDR);
		MDC.put("msgName", msgName == null ? "---" : msgName);
		MDC.put("packetId", packetId == null ? "---" : msgName);
	}

}
