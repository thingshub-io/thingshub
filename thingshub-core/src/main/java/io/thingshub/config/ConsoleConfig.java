package io.thingshub.config;

import java.util.List;

import io.thingshub.ioc.Config;
import io.thingshub.ioc.Value;
import lombok.Getter;

/**
 * <p>
 * 管理控制台配置
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Config
public class ConsoleConfig {

	@Value("${thingshub.console.sso.servers}")
	@Getter
	private List<String> servers;

	@Value("${thingshub.console.sso.username}")
	@Getter
	private String username;

	@Value("${thingshub.console.sso.password}")
	@Getter
	private String password;

	@Value("${thingshub.console.basic.username}")
	@Getter
	private String basicUsername;

	@Value("${thingshub.console.basic.password}")
	@Getter
	private String basicPassword;

}
