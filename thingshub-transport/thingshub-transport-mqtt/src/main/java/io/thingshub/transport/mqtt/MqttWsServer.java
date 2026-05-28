package io.thingshub.transport.mqtt;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.thingshub.transport.Server;
import io.thingshub.transport.Transport;
import io.thingshub.transport.mqtt.handler.ws.MqttOverWsHandler;
import io.thingshub.transport.mqtt.handler.ws.WebSocketOnlyHandler;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;

@Slf4j(topic = "io.thingshub.transport.server")
public class MqttWsServer implements Server {

	@Inject
	private MqttWsTransportConfig config;

	private DisposableServer disposableServer;

	@Override
	public Transport transport() {
		return Transport.MQTT_WS;
	}

	@Override
	public String name() {
		return config.getName();
	}

	@Override
	public Mono<Server> start() {
		return bind(config).doOnNext(this::afterBinding).doOnSuccess(this::onSuccess).doOnError(this::onError).thenReturn(this).cast(Server.class);
	}

	@Override
	public void attachHandlers(Connection connection) {
		connection.addHandlerLast(HttpResponseEncoder.class.getSimpleName(), new HttpResponseEncoder());
		connection.addHandlerLast(HttpRequestDecoder.class.getSimpleName(), new HttpRequestDecoder());
		connection.addHandlerLast(HttpObjectAggregator.class.getSimpleName(), new HttpObjectAggregator(65536));
		connection.addHandlerLast(WebSocketOnlyHandler.class.getSimpleName(), new WebSocketOnlyHandler(config.getPath()));
		connection.addHandlerLast(WebSocketServerProtocolHandler.class.getSimpleName(), new WebSocketServerProtocolHandler(config.getPath(), "mqtt, mqttv3.1, mqttv3.1.1"));
		connection.addHandlerLast(MqttOverWsHandler.class.getSimpleName(), new MqttOverWsHandler(config));
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
		log.info("Thingshub MQTT server over websocket has started on port {}", config.getPort());
	}

	private void onError(Throwable e) {
		log.error("Failed to start MQTT server over websocket. Error:", e);
	}

	@Override
	@PreDestroy
	public void shutdown() {
		if (this.disposableServer != null && !this.disposableServer.isDisposed()) {
			this.disposableServer.dispose();
		}
	}

}