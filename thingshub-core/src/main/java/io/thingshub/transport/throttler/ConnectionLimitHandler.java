package io.thingshub.transport.throttler;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.RateLimiter;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class ConnectionLimitHandler extends ChannelDuplexHandler {

	public interface ChannelPipelineInitializer {
		void initialize(ChannelPipeline pipeline);
	}

	private final RateLimiter rateLimiter;

	public ConnectionLimitHandler(RateLimiter rateLimiter) {
		this.rateLimiter = rateLimiter;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		if (rateLimiter.tryAcquire()) {
			ctx.fireChannelActive();
		} else {
			log.warn("Connection dropped due to exceed limit");

			ctx.channel().config().setAutoRead(false);
			ctx.executor().schedule(() -> {
				if (ctx.channel().isActive()) {
					ctx.close();
				}
			}, ThreadLocalRandom.current().nextLong(100, 3000), TimeUnit.MILLISECONDS);
		}
	}
}