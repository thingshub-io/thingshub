package io.thingshub.transport.tcp;

import io.thingshub.ioc.Config;
import io.thingshub.ioc.Value;
import io.thingshub.transport.TransportConfig;
import lombok.Getter;

@Config
public class TcpTransportConfig extends TransportConfig {

	@Value("${thingshub.transport.tcp.name: TCP Transport Server}")
	@Getter
	private String name;

	@Value("${thingshub.transport.tcp.host: 0.0.0.0}")
	@Getter
	private String host;

	@Value("${thingshub.transport.tcp.port: 5000}")
	@Getter
	private int port;

	@Value("${thingshub.transport.tcp.connect-timeout: 30}")
	@Getter
	private int connectTimeout;// 秒

	@Value("${thingshub.transport.tcp.keep-alive: 300}")
	@Getter
	private Integer keepAlive;// 秒

}
