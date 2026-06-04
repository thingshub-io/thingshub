package io.thingshub.transport.onvif;

import io.thingshub.ioc.Config;
import io.thingshub.ioc.Value;
import io.thingshub.transport.TransportConfig;
import lombok.Getter;

@Config
public class OnvifTransportConfig extends TransportConfig {

	@Value("${thingshub.transport.onvif.name: ONVIF Transport Server}")
	@Getter
	private String serverName;

	@Value("${thingshub.transport.onvif.host: 0.0.0.0}")
	@Getter
	private String host;

	@Value("${thingshub.transport.onvif.port: 2020}")
	@Getter
	private int port;

}
