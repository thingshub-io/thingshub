package io.thingshub.transport.mqtt.codec;

import static io.thingshub.transport.ChannelContextWrapper.ATTRIBUTE_CLIENT_ID;
import static io.thingshub.transport.ChannelContextWrapper.ATTRIBUTE_CLIENT_TYPE;
import static io.thingshub.transport.ChannelContextWrapper.ATTRIBUTE_PRODUCT_CODE;
import static io.thingshub.transport.ChannelContextWrapper.ATTRIBUTE_SCRIPT_LANG;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.MDC;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.thingshub.Broker;
import io.thingshub.commons.model.StreamDirection;
import io.thingshub.ioc.Component;
import io.thingshub.script.ScriptEngine;
import io.thingshub.script.ScriptEngineFactory;
import io.thingshub.script.ScriptException;
import io.thingshub.transport.ChannelContextWrapper;
import io.thingshub.transport.mqtt.MqttChannelContextWrapper;
import io.thingshub.transport.utils.MessageUtils;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Transform custom MQTT payload into standard message model payload
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
@ChannelHandler.Sharable
@Component
public class MessageTransformDecoder extends MessageToMessageDecoder<MqttMessage> {

	@Inject
	private ScriptEngineFactory scriptEngineFactory;

	@Override
	protected void decode(ChannelHandlerContext ctx, MqttMessage msg, List<Object> out) throws Exception {
		switch (msg.fixedHeader().messageType()) {
		case PUBLISH -> {
			MqttPublishMessage publishMessage = (MqttPublishMessage) msg;
			publishMessage.retain();

			try {
				String clientId = ctx.channel().attr(ATTRIBUTE_CLIENT_ID).get();
				byte[] rawPayloadBytes = MessageUtils.readByteBuf(publishMessage.payload());
				MqttChannelContextWrapper.bufferIncoming(
						ctx.channel().id().asLongText().concat("_" + publishMessage.variableHeader().packetId()).concat("_payload"), rawPayloadBytes);

				switch (ctx.channel().attr(ATTRIBUTE_CLIENT_TYPE).get()) {
				case DEVICE -> {
					String scriptLang = ctx.channel().attr(ATTRIBUTE_SCRIPT_LANG).get();
					byte[] payloadBytes = null;
					if (scriptLang != null) {// device send custom message
						String productCode = ctx.channel().attr(ATTRIBUTE_PRODUCT_CODE).get();
						ScriptEngine scriptEngine = scriptEngineFactory.getScriptEngine(scriptLang);

						try {
							String stdPayloadStr = scriptEngine.invoke(productCode, String.class, "decode", clientId, rawPayloadBytes);

							if (log.isDebugEnabled()) {
								setLoggerMdc(ctx, publishMessage);

								log.debug("Message decoded before: \n{}", new String(rawPayloadBytes, StandardCharsets.UTF_8).replace("\n", ""));
								log.debug("Message decoded after: \n{}", stdPayloadStr.replace("\n", ""));
							}

							payloadBytes = stdPayloadStr.getBytes(StandardCharsets.UTF_8);
						} catch (ScriptException se) {
							setLoggerMdc(ctx, publishMessage);
							log.error("", se);

							payloadBytes = rawPayloadBytes;
						}
					} else {
						payloadBytes = rawPayloadBytes;
					}

					ByteBuf payloadBuf4Device = Unpooled.copiedBuffer(payloadBytes);
					out.add(publishMessage.replace(payloadBuf4Device));
					publishMessage.release();
				}
				case CLIENT_USER -> {// business client send standard message
					ByteBuf payloadBuf4SysClient = Unpooled.copiedBuffer(rawPayloadBytes);
					out.add(publishMessage.replace(payloadBuf4SysClient));
				}
				default -> out.add(msg);
				}
			} catch (Exception e) {
				setLoggerMdc(ctx, publishMessage);
				log.error("", e);
			}
		}
		default -> out.add(msg);
		}
	}

	private void setLoggerMdc(ChannelHandlerContext ctx, MqttPublishMessage pubMsg) {
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
		MDC.put("protocol", "MQTT");
		MDC.put("streamDirection", StreamDirection.UP.name());
		MDC.put("serverAddr", Broker.BROKER_ADDR);
		MDC.put("packetId", String.valueOf(pubMsg.variableHeader().packetId()));
		MDC.put("msgName", pubMsg.fixedHeader().messageType().name());
	}

}
