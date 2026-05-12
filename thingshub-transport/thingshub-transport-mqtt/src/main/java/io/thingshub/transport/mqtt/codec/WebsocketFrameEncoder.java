package io.thingshub.transport.mqtt.codec;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.thingshub.ioc.Component;

/**
 * <p>
 * Convert ByteBuf to a WebSocketFrame
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@ChannelHandler.Sharable
@Component
public class WebsocketFrameEncoder extends MessageToMessageEncoder<ByteBuf> {

	@Override
	protected void encode(ChannelHandlerContext chc, ByteBuf bb, List<Object> out) {
		BinaryWebSocketFrame result = new BinaryWebSocketFrame();
		result.content().writeBytes(bb);
		out.add(result);
	}
}