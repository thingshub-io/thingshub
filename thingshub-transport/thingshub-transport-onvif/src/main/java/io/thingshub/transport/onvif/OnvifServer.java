package io.thingshub.transport.onvif;

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
 * ONVIF服务器
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j(topic = "io.thingshub.transport.server")
public class OnvifServer
//implements Server 
{

	@Inject
	private OnvifTransportConfig onvifTransportConfig;

	private DisposableServer disposableServer;

//	@Override
	public Transport transport() {
		return Transport.GB28181_TCP;
	}

//	@Override
	public String name() {
		return onvifTransportConfig.getServerName();
	}

//	@Override
	public Mono<Server> start() {
		return Mono.empty();
//		return bind(onvifTransportConfig).doOnNext(this::afterBinding).doOnSuccess(this::onSuccess).doOnError(this::onError).thenReturn(this).cast(Server.class);
	}

//	@Override
	public void attachHandlers(Connection connection) {
		// TODO
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
		log.info("Thingshub ONVIF transport server has started on port {}", onvifTransportConfig.getPort());
	}

	private void onError(Throwable e) {
		log.error("Failed to start GB28181 TCP transport server. Error: ", e);
	}

//	@Override
	@PreDestroy
	public void shutdown() {
		if (this.disposableServer != null && !this.disposableServer.isDisposed()) {
			this.disposableServer.dispose();
		}
	}

}
