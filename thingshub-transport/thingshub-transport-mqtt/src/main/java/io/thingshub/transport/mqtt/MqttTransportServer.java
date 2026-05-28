package io.thingshub.transport.mqtt;

import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.thingshub.transport.Server;
import io.thingshub.transport.Transport;
import io.thingshub.transport.mqtt.handler.MQTTPreludeHandler;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;

/**
 * <p>
 * MQTT Server
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j(topic = "io.thingshub.transport.server")
public class MqttTransportServer implements Server {

	@Inject
	private MqttTransportConfig mqttTransportConfig;

	private DisposableServer disposableServer;

	@Override
	public Transport transport() {
		return Transport.MQTT;
	}

	@Override
	public String name() {
		return mqttTransportConfig.getName();
	}

	@Override
	public Mono<Server> start() {
		return bind(mqttTransportConfig).doOnNext(this::afterBinding).doOnSuccess(this::onSuccess).doOnError(this::onError).thenReturn(this).cast(Server.class);
	}

	@Override
	public void attachHandlers(Connection connection) {
		connection.addHandlerLast(MqttDecoder.class.getSimpleName(), new MqttDecoder(mqttTransportConfig.getMaxBytesInMessage()));
		connection.addHandlerLast(MqttEncoder.class.getSimpleName(), MqttEncoder.INSTANCE);
		connection.addHandlerLast(MQTTPreludeHandler.class.getSimpleName(), new MQTTPreludeHandler(mqttTransportConfig));
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
		log.info("Thingshub MQTT transport server has started on port {}", mqttTransportConfig.getPort());
	}

	private void onError(Throwable e) {
		log.error("Failed to start MQTT transport server. Error:", e);
	}

	@Override
	@PreDestroy
	public void shutdown() {
		if (this.disposableServer != null && !this.disposableServer.isDisposed()) {
			this.disposableServer.dispose();
		}
	}

}
