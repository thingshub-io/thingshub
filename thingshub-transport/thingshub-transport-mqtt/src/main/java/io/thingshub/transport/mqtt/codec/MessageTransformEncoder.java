package io.thingshub.transport.mqtt.codec;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.MDC;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.thingshub.Broker;
import io.thingshub.commons.model.StreamDirection;
import io.thingshub.ioc.Component;
import io.thingshub.script.ScriptEngine;
import io.thingshub.script.ScriptEngineFactory;
import io.thingshub.transport.ChannelContextWrapper;
import io.thingshub.transport.utils.MessageUtils;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Transform standard message model payload into custom MQTT payload
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
@ChannelHandler.Sharable
@Component
public final class MessageTransformEncoder extends MessageToMessageEncoder<MqttMessage> {

	private ScriptEngineFactory scriptEngineFactory;

	@Inject
	public MessageTransformEncoder(ScriptEngineFactory scriptEngineFactory) {
		this.scriptEngineFactory = scriptEngineFactory;
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, MqttMessage msg, List<Object> out) throws Exception {
		switch (msg.fixedHeader().messageType()) {
		case PUBLISH -> {
			MqttPublishMessage publishMessage = (MqttPublishMessage) msg;
			publishMessage.retain();

			try {
				switch (ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_CLIENT_TYPE).get()) {
				case DEVICE -> {
					String scriptLang = ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_SCRIPT_LANG).get();
					byte[] payloadBytes = null;
					if (scriptLang != null) {
						String productCode = ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_PRODUCT_CODE).get();
						ScriptEngine scriptEngine = scriptEngineFactory.getScriptEngine(scriptLang);
						JSONObject stdPayloadObj = JSON.parseObject(MessageUtils.readByteBuf(publishMessage.payload()));
						String customPayload = scriptEngine.invoke(productCode, String.class, "encode", stdPayloadObj);
						payloadBytes = customPayload.getBytes(StandardCharsets.UTF_8);

						if (log.isDebugEnabled()) {
							setLoggerMdc(ctx, publishMessage);

							log.debug("Client message encoding before: \n{}", stdPayloadObj.toJSONString().replace("\n", ""));
							log.debug("Client message encoding after: \n{}", customPayload.replace("\n", ""));
						}
					} else {
						payloadBytes = MessageUtils.readByteBuf(publishMessage.payload());
					}

					ByteBuf payloadBuf = Unpooled.copiedBuffer(payloadBytes);
					out.add(publishMessage.replace(payloadBuf));
					publishMessage.release();
				}
				case CLIENT_USER -> out.add(publishMessage);
				default -> out.add(publishMessage);
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
		MDC.put("streamDirection", StreamDirection.DOWN.name());
		MDC.put("serverAddr", Broker.BROKER_ADDR);
		MDC.put("packetId", String.valueOf(pubMsg.variableHeader().packetId()));
		MDC.put("msgName", pubMsg.fixedHeader().messageType().name());
	}

}
