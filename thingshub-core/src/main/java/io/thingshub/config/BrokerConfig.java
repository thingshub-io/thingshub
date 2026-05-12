package io.thingshub.config;

import java.util.List;
import java.util.Map;

import io.thingshub.ioc.Config;
import io.thingshub.ioc.Value;
import lombok.Getter;

/**
 * <p>
 * Broker配置
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Config
public class BrokerConfig {

	@Value("${thingshub.node-id}")
	@Getter
	private Integer nodeId = 1;

	@Value("${thingshub.node-attributes}")
	@Getter
	private List<String> nodeAttributes;

	@Value("${thingshub.work-dir}")
	@Getter
	private String workDir;

	@Value("${thingshub.bind}")
	@Getter
	private String bind;

	@Value("${thingshub.port}")
	@Getter
	private Integer port;

	@Value("${thingshub.cluster.addresses}")
	@Getter
	private List<String> addresses;

	@Value("${thingshub.cluster.disc-port}")
	@Getter
	private Integer discPort = 47500;

	@Value("${thingshub.cluster.comm-port}")
	@Getter
	private Integer commPort = 47100;

	@Value("${thingshub.logging.level}")
	@Getter
	private Map<String, String> loggers;

}
