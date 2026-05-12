package io.thingshub.dashboard.server;

import io.thingshub.ioc.Config;
import io.thingshub.ioc.Value;
import lombok.Getter;

@Config
public class DashboardConfig {

	@Value("${thingshub.dashboard.host: 0.0.0.0}")
	@Getter
	private String host;

	@Value("${thingshub.dashboard.port: 18080}")
	@Getter
	private int port;

	@Value("${thingshub.dashboard.access-log: true}")
	@Getter
	private boolean accessLog;

}
