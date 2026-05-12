package io.thingshub.transport.throttler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectionRejectHandler extends ChannelInboundHandlerAdapter {

	private final Condition rejectCondition;

	public ConnectionRejectHandler(Condition rejectCondition) {
		this.rejectCondition = rejectCondition;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		if (rejectCondition.meet()) {
			log.warn("Reject connection due to {}", rejectCondition);

			ctx.channel().config().setAutoRead(false);
			ctx.close();

			return;
		}

		super.channelActive(ctx);
	}
}
