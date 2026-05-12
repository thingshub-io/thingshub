package io.thingshub.transport.http;

import static io.thingshub.transport.throttler.ORCondition.or;

import com.google.common.util.concurrent.RateLimiter;

import io.netty.channel.ChannelOption;
import io.thingshub.transport.Server;
import io.thingshub.transport.Transport;
import io.thingshub.transport.http.handler.HttpTransportRequestHandler;
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
 * HTTP Transport Server
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j(topic = "io.thingshub.transport.server")
public class HttpTransportServer implements Server {

	@Inject
	private HttpTransportConfig config;

	@Inject
	private HttpTransportRequestHandler requestHandler;

	private DisposableServer disposableServer;

	@Override
	public Transport transport() {
		return Transport.HTTP;
	}

	@Override
	public String name() {
		return config.getName();
	}

	@Override
	public Mono<Server> start() {
		return bind().doOnNext(this::afterBinding).doOnSuccess(this::onSuccess).doOnError(this::onError).thenReturn(this).cast(Server.class);
	}

	@SuppressWarnings("unchecked")
	public Mono<DisposableServer> bind() {
		return Mono.deferContextual(contextView -> {
			LoopResources loopResources = LoopResources.create("thingshub-" + config.getPort(), config.getBossThreads(), config.getWorkerThreads(), true);
			RateLimiter connRateLimiter = RateLimiter.create(config.getConnectRateLimit());

			reactor.netty.http.server.HttpServer httpServer = reactor.netty.http.server.HttpServer.create();

			if (config.getSsl() != null) {
				httpServer.secure(sslContextSpec -> this.secure(sslContextSpec, config.getSsl()));
			}
			if (config.getOptions() != null) {
				config.getOptions().forEach((k, v) -> httpServer.option((ChannelOption<? super Object>) k, v));
			}
			if (config.getChildOptions() != null) {
				config.getChildOptions().forEach((k, v) -> httpServer.childOption((ChannelOption<? super Object>) k, v));
			}

			return Mono.just(httpServer.host(config.getHost()).port(config.getPort()).wiretap(config.isWiretap()).route(requestHandler)
					.accessLog(config.isAccessLog(),
							args -> AccessLog.create("[{}] [{}] [{}] [{}]", args.method(), args.uri(), args.status(), args.contentLength()))
					.runOn(loopResources).doOnChannelInit((observer, channel, remoteAddress) -> {
						if (!connRateLimiter.tryAcquire()) {
							log.debug("Connection dropped due to exceed limit");
							channel.config().setAutoRead(false);
							if (channel.isActive()) {
								channel.close();
							}

							return;
						}

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
		log.info("Thingshub HTTP transport server has started on port {}", config.getPort());
	}

	private void onError(Throwable e) {
		log.error("Failed to start HTTP transport server. Error:", e);
	}

	@Override
	@PreDestroy
	public void shutdown() {
		if (this.disposableServer != null && !this.disposableServer.isDisposed()) {
			this.disposableServer.dispose();
		}
	}

}
