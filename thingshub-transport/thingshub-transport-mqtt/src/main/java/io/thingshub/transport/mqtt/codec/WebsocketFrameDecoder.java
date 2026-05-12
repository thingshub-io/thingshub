package io.thingshub.transport.mqtt.codec;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.thingshub.ioc.Component;

/**
 * <p>
 * Convert WebSocketFrame to a ByteBuf
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@ChannelHandler.Sharable
@Component
public class WebsocketFrameDecoder extends MessageToMessageDecoder<BinaryWebSocketFrame> {

	@Override
	protected void decode(ChannelHandlerContext chc, BinaryWebSocketFrame frame, List<Object> out) {
		ByteBuf bb = frame.content();
		bb.retain();
		out.add(bb);
	}
}