package io.thingshub.transport.mqtt;

import io.thingshub.ioc.Config;
import io.thingshub.ioc.Value;
import lombok.Getter;

@Config
public class MqttWsServerConfig extends MqttTransportConfig {

	@Value("${thingshub.server.mqtt.ws.name: MQTT over WebSocket}")
	@Getter
	private String name;

	@Value("${thingshub.server.mqtt.ws.host: 0.0.0.0}")
	@Getter
	private String host;

	@Value("${thingshub.server.mqtt.ws.port: 8883}")
	@Getter
	private int port;

	@Value("${thingshub.server.mqtt.ws.path: /mqtt}")
	@Getter
	private String path;

	@Value("${thingshub.server.mqtt.ws.connect-timeout: 20}")
	@Getter
	private int connectTimeout;

	@Value("${thingshub.server.mqtt.ws.keep-alive: 300}")
	@Getter
	private int keepAlive;

}
