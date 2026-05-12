package io.thingshub.transport.throttler;

import java.util.LinkedList;
import java.util.Queue;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;

public class MessageDebounceHandler extends ChannelDuplexHandler {

	private final Queue<Object> buffer = new LinkedList<>();

	private boolean readOne = false;

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Object msg;
		while ((msg = buffer.poll()) != null) {
			ReferenceCountUtil.release(msg);
		}

		super.channelInactive(ctx);
	}

	@Override
	public void read(ChannelHandlerContext ctx) {
		if (ctx.channel().config().isAutoRead()) {
			Object msg;
			while ((msg = buffer.poll()) != null) {
				ctx.fireChannelRead(msg);
			}
			ctx.read();
		} else {
			Object msg = buffer.poll();
			if (msg != null) {
				ctx.fireChannelRead(msg);
			} else {
				readOne = true;
				ctx.read();
			}
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		if (ctx.channel().config().isAutoRead()) {
			ctx.fireChannelRead(msg);
		} else {
			buffer.offer(msg);
			if (readOne) {
				Object theMsg = buffer.poll();
				ctx.fireChannelRead(theMsg);
				readOne = false;
			}
		}
	}
}
