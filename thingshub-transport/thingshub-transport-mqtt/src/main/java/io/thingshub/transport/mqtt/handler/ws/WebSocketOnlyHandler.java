package io.thingshub.transport.mqtt.handler.ws;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * <p>
 * Only accept that requests that are WebSocket upgrade
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public class WebSocketOnlyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private final String websocketPath;

	public WebSocketOnlyHandler(String websocketPath) {
		super(false);
		this.websocketPath = websocketPath;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
		if (!req.uri().equals(websocketPath) || !req.headers().get(HttpHeaderNames.UPGRADE, "").equalsIgnoreCase("websocket")) {
			FullHttpResponse response = new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.BAD_REQUEST);
			ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
		} else {
			ctx.pipeline().remove(this);
			ctx.fireChannelRead(req);
		}
	}
}