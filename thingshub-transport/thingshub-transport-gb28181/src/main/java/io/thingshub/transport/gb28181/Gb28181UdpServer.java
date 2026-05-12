package io.thingshub.transport.gb28181;

import lombok.extern.slf4j.Slf4j;
import reactor.netty.Connection;

/**
 * <p>
 * GB28181 UDP服务器
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j(topic = "io.thingshub.transport.server")
public class Gb28181UdpServer
//implements Server 
{

	private Gb28181TransportConfig config;

	private Connection connection;

//	@Override
//	public TransportType transport() {
//		return TransportType.GB28181_UDP;
//	}

//	@Override
//	public String getName() {
//		return gb28181ServerConfig.getUdpServerName();
//	}

//	@Override
//	public Mono<Server> start() {
//		return gb28181UdpTransport.bind() //
//				.doOnNext(conn -> {
//					connection = conn;
//					Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//						if (!connection.isDisposed())
//							connection.dispose();
//					}));
//				}) //
//				.thenReturn(this) //
//				.doOnSuccess(t -> log.info("Thingshub GB28181 UDP Server has started on port {}", gb28181ServerConfig.getPort())) //
//				.doOnError(e -> log.error("Failed to start GB28181 UDP Server. Error: ", e)) //
//				.cast(Server.class);
//	}

//	@Override
//	@PreDestroy
//	public void shutdown() {
//		if (!this.connection.isDisposed())
//			this.connection.dispose();
//	}

}
