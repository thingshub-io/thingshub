package io.thingshub.transport.gb28181;

import io.thingshub.ioc.Config;
import io.thingshub.ioc.Value;
import io.thingshub.transport.TransportConfig;
import lombok.Getter;

@Config
public class Gb28181TransportConfig extends TransportConfig {

	@Value("${thingshub.transport.gb28181.tcp.name: GB28181 TCP Transport Server}")
	@Getter
	private String tcpServerName;

	@Value("${thingshub.transport.gb28181.udp.name: GB28181 UDP Transport Server}")
	@Getter
	private String udpServerName;

	@Value("${thingshub.transport.gb28181.host: 0.0.0.0}")
	@Getter
	private String host;

	@Value("${thingshub.transport.gb28181.port: 5060}")
	@Getter
	private int port;

}
