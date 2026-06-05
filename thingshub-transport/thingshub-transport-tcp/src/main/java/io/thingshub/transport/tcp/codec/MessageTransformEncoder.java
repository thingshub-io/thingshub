package io.thingshub.transport.tcp.codec;

import java.nio.charset.Charset;
import java.util.List;

import com.alibaba.fastjson2.JSON;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.thingshub.commons.ThingshubMessage;
import io.thingshub.ioc.Component;
import io.thingshub.script.ScriptEngine;
import io.thingshub.script.ScriptEngineFactory;
import io.thingshub.transport.ChannelContextWrapper;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Transform standard message model payload into custom TCP payload
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
@ChannelHandler.Sharable
@Component
public final class MessageTransformEncoder extends MessageToMessageEncoder<ThingshubMessage> {

	private ScriptEngineFactory scriptEngineFactory;

	@Inject
	public MessageTransformEncoder(ScriptEngineFactory scriptEngineFactory) {
		this.scriptEngineFactory = scriptEngineFactory;
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, ThingshubMessage replyMsg, List<Object> out) throws Exception {
		out.add(doEncode(ctx, replyMsg));
	}

	private ByteBuf doEncode(ChannelHandlerContext ctx, ThingshubMessage replyMsg) {
		// TODO 将消息转换成ThingMessage
		String deviceSn = ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_CLIENT_ID).get();

		try {
			String productCode = ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_PRODUCT_CODE).get();
			String scriptLang = ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_SCRIPT_LANG).get();

			ScriptEngine scriptEngine = scriptEngineFactory.getScriptEngine(scriptLang);
			String msgStr = scriptEngine.invoke(productCode, String.class, "encode", deviceSn, replyMsg);

			if (log.isDebugEnabled()) {
				log.debug("TcpTransformEncoder invoke function {} to transform message, standard message: {}, custom message: {}", "encode", JSON.toJSONString(replyMsg), msgStr);
			}

			byte[] bytes = msgStr.getBytes(Charset.forName("UTF-8"));
			ByteBuf buf = ctx.alloc().buffer(bytes.length);
			buf.writeBytes(bytes);

			return buf;
		} catch (Throwable e) {
			log.error("Failed to encode TCP transport message. Devcie SN: {}, Error: ", deviceSn, e);
			throw e;
		}
	}

}
