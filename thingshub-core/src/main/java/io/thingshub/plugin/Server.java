package io.thingshub.plugin;

import java.io.File;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.thingshub.config.SslConfig;
import io.thingshub.transport.TransportConfig;
import io.thingshub.transport.Transport;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.SslProvider;

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

	Mono<DisposableServer> bind(TransportConfig config);

	default void attachHandlers(Connection connection) {
	};

	void shutdown();

}
