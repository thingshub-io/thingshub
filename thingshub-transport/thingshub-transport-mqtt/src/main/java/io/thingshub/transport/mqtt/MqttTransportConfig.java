package io.thingshub.transport.mqtt;

import io.thingshub.ioc.Config;
import io.thingshub.ioc.Value;
import io.thingshub.transport.TransportConfig;
import lombok.Getter;

@Config
public class MqttTransportConfig extends TransportConfig {

	@Value("${thingshub.transport.mqtt.name: MQTT Server}")
	@Getter
	private String name;

	@Value("${thingshub.transport.mqtt.host: 0.0.0.0}")
	@Getter
	private String host;

	@Value("${thingshub.transport.mqtt.port: 1883}")
	@Getter
	private int port;

	@Value("${thingshub.transport.mqtt.connect-timeout: 20}")
	@Getter
	private int connectTimeout;

	@Value("${thingshub.transport.mqtt.keep-alive: 300}")
	@Getter
	private int keepAlive;

}
