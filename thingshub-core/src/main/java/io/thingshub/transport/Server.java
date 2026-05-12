package io.thingshub.transport;

import static io.thingshub.transport.throttler.ORCondition.or;

import java.io.File;

import com.google.common.util.concurrent.RateLimiter;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.thingshub.config.SslConfig;
import io.thingshub.transport.throttler.Condition;
import io.thingshub.transport.throttler.DirectMemPressureCondition;
import io.thingshub.transport.throttler.HeapMemPressureCondition;
import io.thingshub.transport.throttler.MessageDebounceHandler;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpServer;

/**
 * <p>
 * Transport Server
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public interface Server {

	Transport transport();

	String name();

	@SuppressWarnings("deprecation")
	default void secure(SslProvider.SslContextSpec sslContextSpec, SslConfig sslConfig) {
		SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(new File(sslConfig.getCrt()), new File(sslConfig.getKey()));
		if (sslConfig.getCa() != null && !sslConfig.getCa().isBlank()) {
			sslContextBuilder = sslContextBuilder.trustManager(new File(sslConfig.getCa()));
			sslContextBuilder.clientAuth(ClientAuth.REQUIRE);
		}

		sslContextSpec.sslContext(sslContextBuilder);
	};

	Mono<Server> start();

	@SuppressWarnings("unchecked")
	default Mono<DisposableServer> bind(TransportConfig config) {
		return Mono.deferContextual(contextView -> {
			LoopResources loopResources = LoopResources.create("thingshub-" + config.getPort(), config.getBossThreads(), config.getWorkerThreads(), true);
			RateLimiter connRateLimiter = RateLimiter.create(config.getConnectRateLimit());

			TcpServer tcpServer = TcpServer.create();
			if (config.getSsl() != null) {
				tcpServer.secure(sslContextSpec -> this.secure(sslContextSpec, config.getSsl()));
			}
			if (config.getOptions() != null) {
				config.getOptions().forEach((k, v) -> tcpServer.option((ChannelOption<? super Object>) k, v));
			}
			if (config.getChildOptions() != null) {
				config.getChildOptions().forEach((k, v) -> tcpServer.childOption((ChannelOption<? super Object>) k, v));
			}

			return Mono.just(tcpServer.host(config.getHost()).port(config.getPort()).wiretap(config.isWiretap()).runOn(loopResources)
					.doOnChannelInit((observer, channel, remoteAddress) -> {
						if (!connRateLimiter.tryAcquire()) {
							channel.config().setAutoRead(false);
							if (channel.isActive()) {
								channel.close();
							}

							return;
						}

						Condition rejectCondition = or(DirectMemPressureCondition.INSTANCE, HeapMemPressureCondition.INSTANCE);
						if (rejectCondition.meet()) {
							channel.config().setAutoRead(false);
							if (channel.isActive()) {
								channel.close();
							}

							return;
						}
					}).doOnConnection(connection -> {
						connection.addHandlerLast(new ChannelTrafficShapingHandler(config.getWriteLimit(), config.getReadLimit()));
						connection.addHandlerLast(new MessageDebounceHandler());

						attachHandlers(connection);
					}).handle((inbound, outbound) -> inbound.receive().then()));
		}).flatMap(view -> view.bind().cast(DisposableServer.class));
	};

	default void attachHandlers(Connection connection) {
	};

	void shutdown();

}
