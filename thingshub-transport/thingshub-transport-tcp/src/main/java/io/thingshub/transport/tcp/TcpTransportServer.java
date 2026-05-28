package io.thingshub.transport.tcp;

import io.thingshub.transport.Server;
import io.thingshub.transport.Transport;
import io.thingshub.transport.tcp.handler.TcpPreludeHandler;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;

/**
 * <p>
 * TCP服务器
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j(topic = "io.thingshub.transport.server")
public class TcpTransportServer implements Server {

	@Inject
	private TcpTransportConfig tcpTransportConfig;

	private DisposableServer disposableServer;

	@Override
	public Transport transport() {
		return Transport.TCP;
	}

	@Override
	public String name() {
		return tcpTransportConfig.getName();
	}

	@Override
	public Mono<Server> start() {
		return bind(tcpTransportConfig).doOnNext(this::afterBinding).doOnSuccess(this::onSuccess).doOnError(this::onError).thenReturn(this).cast(Server.class);
	}

	@Override
	public void attachHandlers(Connection connection) {
		connection.addHandlerLast(TcpPreludeHandler.class.getSimpleName(), new TcpPreludeHandler(tcpTransportConfig));
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
		log.info("Thingshub TCP transport server has started on port {}", tcpTransportConfig.getPort());
	}

	private void onError(Throwable e) {
		log.error("Failed to start TCP transport server. Error: ", e);
	}

	@Override
	@PreDestroy
	public void shutdown() {
		if (this.disposableServer != null && !this.disposableServer.isDisposed()) {
			this.disposableServer.dispose();
		}
	}

}
