package io.thingshub.transport.gb28181;

import io.sipstack.netty.codec.sip.SipMessageDatagramDecoder;
import io.sipstack.netty.codec.sip.SipMessageEncoder;
import io.thingshub.transport.Server;
import io.thingshub.transport.Transport;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;

/**
 * <p>
 * Gb28181信令服务器
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j(topic = "io.thingshub.transport.server")
public class Gb28181TcpServer implements Server {

	@Inject
	private Gb28181TransportConfig config;

	private DisposableServer disposableServer;

	@Override
	public Transport transport() {
		return Transport.GB28181_TCP;
	}

	@Override
	public String name() {
		return config.getTcpServerName();
	}

	@Override
	public Mono<Server> start() {
		return bind(config).doOnNext(this::afterBinding).doOnSuccess(this::onSuccess).doOnError(this::onError).thenReturn(this).cast(Server.class);
	}

	@Override
	public void attachHandlers(Connection connection) {
		connection.addHandlerLast(SipMessageDatagramDecoder.class.getSimpleName(), new SipMessageDatagramDecoder());
		connection.addHandlerLast(SipMessageEncoder.class.getSimpleName(), new SipMessageEncoder());
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
		log.info("Thingshub GB28181 TCP transport server has started on port {}", config.getPort());
	}

	private void onError(Throwable e) {
		log.error("Failed to start GB28181 TCP transport server. Error: ", e);
	}

	@Override
	@PreDestroy
	public void shutdown() {
		if (this.disposableServer != null && !this.disposableServer.isDisposed()) {
			this.disposableServer.dispose();
		}
	}

}
