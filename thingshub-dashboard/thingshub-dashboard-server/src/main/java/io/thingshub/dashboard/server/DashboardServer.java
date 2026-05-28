package io.thingshub.dashboard.server;

import static io.thingshub.transport.throttler.ORCondition.or;

import java.util.Set;

import io.thingshub.http.interceptor.Interceptor;
import io.thingshub.transport.Server;
import io.thingshub.transport.Transport;
import io.thingshub.transport.throttler.Condition;
import io.thingshub.transport.throttler.DirectMemPressureCondition;
import io.thingshub.transport.throttler.HeapMemPressureCondition;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.logging.AccessLog;
import reactor.netty.resources.LoopResources;

/**
 * <p>
 * Dashboard Server
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class DashboardServer implements Server {

	@Inject
	private DashboardConfig config;

	@Inject
	private DashboardRequestHandler requestHandler;

	private DisposableServer disposableServer;

	@Override
	public Transport transport() {
		return Transport.HTTP;
	}

	@Override
	public String name() {
		return "Dashboard Server";
	}

	@Override
	public Mono<Server> start() {
		return bind().doOnNext(this::afterBinding).doOnSuccess(this::onSuccess).doOnError(this::onError).thenReturn(this).cast(Server.class);
	}

	public Mono<DisposableServer> bind() {
		return Mono.deferContextual(contextView -> {
			LoopResources loopResources = LoopResources.create("thingshub-" + config.getPort(), 1, Math.max(Runtime.getRuntime().availableProcessors() * 2, 4),
					true);
//			RateLimiter connRateLimiter = RateLimiter.create(config.getConnectRateLimit());

			reactor.netty.http.server.HttpServer httpServer = reactor.netty.http.server.HttpServer.create();

//			if (config.getSsl() != null) {
//				httpServer.secure(sslContextSpec -> this.secure(sslContextSpec, config.getSsl()));
//			}
//			if (config.getOptions() != null) {
//				config.getOptions().forEach((k, v) -> httpServer.option((ChannelOption<? super Object>) k, v));
//			}
//			if (config.getChildOptions() != null) {
//				config.getChildOptions().forEach((k, v) -> httpServer.childOption((ChannelOption<? super Object>) k, v));
//			}

			return Mono.just(httpServer.host(config.getHost()).port(config.getPort()).route(requestHandler)
					.accessLog(config.isAccessLog(),
							args -> AccessLog.create("[{}] [{}] [{}] [{}]", args.method(), args.uri(), args.status(), args.contentLength()))
					.runOn(loopResources).doOnChannelInit((observer, channel, remoteAddress) -> {
						Condition rejectCondition = or(DirectMemPressureCondition.INSTANCE, HeapMemPressureCondition.INSTANCE);
						if (rejectCondition.meet()) {
							log.debug("Reject connection due to {}", rejectCondition);

							channel.config().setAutoRead(false);
							if (channel.isActive()) {
								channel.close();
							}

							return;
						}
					}));
		}).flatMap(view -> view.bind().cast(DisposableServer.class));
	}

	private void afterBinding(DisposableServer disposableServer) {
		this.disposableServer = disposableServer;
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (!disposableServer.isDisposed()) {
				disposableServer.dispose();
			}
		}));
	}

	private void onSuccess(DisposableServer disposableServer) {
		log.info("Thingshub Dashboard Server has started on port {}", config.getPort());
	}

	private void onError(Throwable e) {
		log.error("Failed to start Dashboard Server. Error:", e);
	}

	@Override
	@PreDestroy
	public void shutdown() {
		if (this.disposableServer != null && !this.disposableServer.isDisposed()) {
			this.disposableServer.dispose();
		}
	}

}
