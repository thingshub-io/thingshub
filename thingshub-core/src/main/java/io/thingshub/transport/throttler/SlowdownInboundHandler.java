package io.thingshub.transport.throttler;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Ticker;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class SlowdownInboundHandler extends ChannelInboundHandlerAdapter {

	private static final long MAX_SLOWDOWN_TIME = Duration.ofSeconds(5).toNanos();// max_slowdown_timeout_seconds

	private final Condition slowDownCondition;

	private ChannelHandlerContext ctx;

	private ScheduledFuture<?> resumeTask;

	private long slowDownAt = Long.MAX_VALUE;

	public SlowdownInboundHandler(Condition slowDownCondition) {
		this.slowDownCondition = slowDownCondition;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		super.handlerAdded(ctx);
		this.ctx = ctx;
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		if (resumeTask != null) {
			resumeTask.cancel(true);
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		if (slowDownCondition.meet()) {
			ctx.channel().config().setAutoRead(false);
			slowDownAt = Ticker.systemTicker().read();
			scheduleResumeRead();
		}
		ctx.fireChannelRead(msg);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		if (!slowDownCondition.meet()) {
			ctx.channel().config().setAutoRead(true);
			slowDownAt = Long.MAX_VALUE;
		} else {
			closeIfNeeded();
		}
		ctx.fireChannelReadComplete();
	}

	private void scheduleResumeRead() {
		if (resumeTask == null || resumeTask.isDone()) {
			resumeTask = ctx.executor().schedule(this::resumeRead, ThreadLocalRandom.current().nextLong(100, 1001), TimeUnit.MILLISECONDS);
		}
	}

	private void resumeRead() {
		if (!slowDownCondition.meet()) {
			if (!ctx.channel().config().isAutoRead()) {
				ctx.channel().config().setAutoRead(true);
				ctx.read();
				slowDownAt = Long.MAX_VALUE;
			}
		} else {
			if (!closeIfNeeded()) {
				resumeTask = null;
				scheduleResumeRead();
			}
		}
	}

	private boolean closeIfNeeded() {
		if (Ticker.systemTicker().read() - slowDownAt > MAX_SLOWDOWN_TIME) {
			ctx.close();

			return true;
		}

		return false;
	}
}