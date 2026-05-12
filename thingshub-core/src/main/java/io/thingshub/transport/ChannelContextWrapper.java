package io.thingshub.transport;

import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import io.thingshub.config.TenantSettings;
import io.thingshub.service.model.ClientType;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChannelContextWrapper implements ChannelHandlerContext {

	public static final AttributeKey<String> ATTRIBUTE_CURRENT_SERVER_NAME = AttributeKey.valueOf("attr_current_server_name");

	public static final AttributeKey<String> ATTRIBUTE_CLIENT_ID = AttributeKey.valueOf("attr_client_id");

	public static final AttributeKey<ClientType> ATTRIBUTE_CLIENT_TYPE = AttributeKey.valueOf("attr_client_type");

	public static final AttributeKey<String> ATTRIBUTE_PRODUCT_CODE = AttributeKey.valueOf("attr_prod_code");

	public static final AttributeKey<String> ATTRIBUTE_SCRIPT_LANG = AttributeKey.valueOf("attr_script_lang");

	private static final Cache<String, Object> INCOMING_BUFFER = Caffeine.newBuilder().softValues().build();

	private static final Cache<String, Object> OUTGOING_BUFFER = Caffeine.newBuilder().softValues().build();

	private final ChannelHandlerContext ctx;

	private final int MAX_MESSAGE_PACKETID = 65535;

	private final AtomicInteger MESSAGE_PACKETID_GETTER = new AtomicInteger(0);

	private final int DEFAULT_FLUSH_THRESHOLD = 128;

	private final Set<CompletableFuture<?>> fgTasks = new HashSet<>();

	private final Runnable flushTask;

	private Future<?> nextScheduledFlush;

	private int flushPendingCount;

	private TenantSettings tenantSettings;

	private String clientId;

	private String clientAddr;

	private String protocolVersion;

	private String channelId;

	private int sessionExpiryInterval;

	public static void bufferIncoming(String packetKey, Object incoming) {
		INCOMING_BUFFER.put(packetKey, incoming);
	}

	public static Object pollIncoming(String packetKey) {
		Object incoming = INCOMING_BUFFER.getIfPresent(packetKey);
		INCOMING_BUFFER.invalidate(packetKey);

		return incoming;
	}

	public static Object getIncoming(String packetKey) {
		return INCOMING_BUFFER.getIfPresent(packetKey);
	}

	public static void bufferOutgoing(String packetKey, Object outgoing) {
		OUTGOING_BUFFER.put(packetKey, outgoing);
	}

	public static Object pollOutgoing(String packetKey) {
		Object outgoing = OUTGOING_BUFFER.getIfPresent(packetKey);
		OUTGOING_BUFFER.invalidate(packetKey);

		return outgoing;
	}

	public static Object getOutgoing(String packetKey) {
		return OUTGOING_BUFFER.getIfPresent(packetKey);
	}

	public ChannelContextWrapper(@NonNull ChannelHandlerContext ctx, TenantSettings tenantSettings, String clientId, String clientAddr, int keepalive,
			String protocolVersion, int sessionExpiryInterval) {
		this.ctx = ctx;

		this.tenantSettings = tenantSettings;
		this.clientId = clientId;
		this.clientAddr = clientAddr;
		this.protocolVersion = protocolVersion;
		this.sessionExpiryInterval = sessionExpiryInterval;
		this.channelId = ctx.channel().id().asLongText();

		this.flushTask = () -> {
			if (flushPendingCount > 0) {
				flushPendingCount = 0;
				nextScheduledFlush = null;
				ctx.flush();
			}
		};
	}

	public int nextPacketId() {
		int index = MESSAGE_PACKETID_GETTER.incrementAndGet();
		if (index > MAX_MESSAGE_PACKETID) {
			synchronized (this) {
				index = MESSAGE_PACKETID_GETTER.incrementAndGet();
				if (index > MAX_MESSAGE_PACKETID) {
					index = 1;
					MESSAGE_PACKETID_GETTER.set(index);
				}
			}
		}

		return index;
	}

	public TenantSettings getTenantSettings() {
		return tenantSettings;
	}

	public String getClientId() {
		return clientId;
	}

	public String getClientAddr() {
		return clientAddr;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public int getSessionExpiryInterval() {
		return sessionExpiryInterval;
	}

	public String getChannelId() {
		return channelId;
	}

	public Set<CompletableFuture<?>> getFgTasks() {
		return fgTasks;
	}

	public void flushIfNeeded() {
		if (flushPendingCount > 0) {
			if (nextScheduledFlush != null) {
				nextScheduledFlush.cancel(false);
				nextScheduledFlush = null;
			}

			flushPendingCount = 0;
			this.ctx.flush();
		}
	}

	public final <T> CompletableFuture<T> addFgTask(CompletableFuture<T> taskFuture) {
		if (!taskFuture.isDone()) {
			fgTasks.add(taskFuture);
			taskFuture.whenComplete((v, e) -> {
				if (e != null) {
					log.error("", e);
				}
				fgTasks.remove(taskFuture);
			});
		}

		return taskFuture;
	}

	public void goAway(boolean immediately) {
		if (!ctx.channel().isActive()) {
			return;
		}

		ctx.channel().config().setAutoRead(false);
		if (immediately) {
			ctx.close();
		} else {
			ctx.executor().schedule(() -> ctx.close(), ThreadLocalRandom.current().nextInt(100, 3000), TimeUnit.MILLISECONDS);
		}
	}

	public void goAwayWithFarewell(Object farewell, boolean immediately) {
		if (!ctx.channel().isActive()) {
			return;
		}

		ctx.channel().config().setAutoRead(false);
		if (immediately) {
			ctx.writeAndFlush(farewell).addListener(ChannelFutureListener.CLOSE);
		} else {
			ctx.executor().schedule(() -> ctx.writeAndFlush(farewell).addListener(ChannelFutureListener.CLOSE), ThreadLocalRandom.current().nextInt(100, 3000),
					TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public ChannelFuture bind(SocketAddress localAddress) {
		return this.ctx.bind(localAddress);
	}

	@Override
	public ChannelFuture connect(SocketAddress remoteAddress) {
		return this.ctx.connect(remoteAddress);
	}

	@Override
	public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
		return this.ctx.connect(remoteAddress, localAddress);
	}

	@Override
	public ChannelFuture disconnect() {
		return this.ctx.disconnect();
	}

	@Override
	public ChannelFuture close() {
		return this.ctx.close();
	}

	@Override
	public ChannelFuture deregister() {
		return this.ctx.deregister();
	}

	@Override
	public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
		return this.ctx.bind(localAddress, promise);
	}

	@Override
	public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
		return this.ctx.connect(remoteAddress, promise);
	}

	@Override
	public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
		return this.ctx.connect(remoteAddress, localAddress, promise);
	}

	@Override
	public ChannelFuture disconnect(ChannelPromise promise) {
		return this.ctx.disconnect(promise);
	}

	@Override
	public ChannelFuture close(ChannelPromise promise) {
		return this.ctx.close(promise);
	}

	@Override
	public ChannelFuture deregister(ChannelPromise promise) {
		return this.ctx.deregister(promise);
	}

	@Override
	public ChannelFuture write(Object msg) {
		return this.ctx.write(msg);
	}

	@Override
	public ChannelFuture write(Object msg, ChannelPromise promise) {
		return this.ctx.write(msg, promise);
	}

	@Override
	public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
		return this.ctx.writeAndFlush(msg, promise);
	}

	@Override
	public ChannelFuture writeAndFlush(Object msg) {
		ChannelFuture future = this.ctx.write(msg);
		if (++flushPendingCount == DEFAULT_FLUSH_THRESHOLD) {// flush immediately
			if (nextScheduledFlush != null) {
				nextScheduledFlush.cancel(false);
				nextScheduledFlush = null;
			}

			flushPendingCount = 0;
			ctx.flush();
		} else {
			if (nextScheduledFlush == null) {
				nextScheduledFlush = this.ctx.executor().submit(flushTask);
			}
		}

		return future;
	}

	@Override
	public ChannelPromise newPromise() {
		return this.ctx.newPromise();
	}

	@Override
	public ChannelProgressivePromise newProgressivePromise() {
		return this.ctx.newProgressivePromise();
	}

	@Override
	public ChannelFuture newSucceededFuture() {
		return this.ctx.newSucceededFuture();
	}

	@Override
	public ChannelFuture newFailedFuture(Throwable cause) {
		return this.ctx.newFailedFuture(cause);
	}

	@Override
	public ChannelPromise voidPromise() {
		return this.ctx.voidPromise();
	}

	@Override
	public Channel channel() {
		return this.ctx.channel();
	}

	@Override
	public EventExecutor executor() {
		return this.ctx.executor();
	}

	@Override
	public String name() {
		return this.ctx.name();
	}

	@Override
	public ChannelHandler handler() {
		return this.ctx.handler();
	}

	@Override
	public boolean isRemoved() {
		return this.ctx.isRemoved();
	}

	@Override
	public ChannelHandlerContext fireChannelRegistered() {
		return this.ctx.fireChannelRegistered();
	}

	@Override
	public ChannelHandlerContext fireChannelUnregistered() {
		return this.ctx.fireChannelUnregistered();
	}

	@Override
	public ChannelHandlerContext fireChannelActive() {
		return this.ctx.fireChannelActive();
	}

	@Override
	public ChannelHandlerContext fireChannelInactive() {
		return this.ctx.fireChannelInactive();
	}

	@Override
	public ChannelHandlerContext fireExceptionCaught(Throwable cause) {
		return this.ctx.fireExceptionCaught(cause);
	}

	@Override
	public ChannelHandlerContext fireUserEventTriggered(Object evt) {
		return this.ctx.fireUserEventTriggered(evt);
	}

	@Override
	public ChannelHandlerContext fireChannelRead(Object msg) {
		return this.ctx.fireChannelRead(msg);
	}

	@Override
	public ChannelHandlerContext fireChannelReadComplete() {
		return this.ctx.fireChannelReadComplete();
	}

	@Override
	public ChannelHandlerContext fireChannelWritabilityChanged() {
		return this.ctx.fireChannelWritabilityChanged();
	}

	@Override
	public ChannelHandlerContext read() {
		return this.ctx.read();
	}

	@Override
	public ChannelHandlerContext flush() {
		return this.ctx.flush();
	}

	@Override
	public ChannelPipeline pipeline() {
		return this.ctx.pipeline();
	}

	@Override
	public ByteBufAllocator alloc() {
		return this.ctx.alloc();
	}

	@Deprecated
	@Override
	public <T> Attribute<T> attr(AttributeKey<T> key) {
		return this.ctx.attr(key);
	}

	@Deprecated
	@Override
	public <T> boolean hasAttr(AttributeKey<T> key) {
		return this.ctx.hasAttr(key);
	}

}
