package io.thingshub.transport.http;

import io.thingshub.ioc.Config;
import io.thingshub.ioc.Value;
import io.thingshub.transport.TransportConfig;
import lombok.Getter;

@Config
public class HttpTransportConfig extends TransportConfig {

	@Value("${thingshub.transport.http.name: HTTP Transport}")
	@Getter
	private String name;

	@Value("${thingshub.transport.http.host: 0.0.0.0}")
	@Getter
	private String host;

	@Value("${thingshub.transport.http.port: 3880}")
	@Getter
	private int port;

	@Value("${thingshub.transport.http.access-log: true}")
	@Getter
	private boolean accessLog;

}
