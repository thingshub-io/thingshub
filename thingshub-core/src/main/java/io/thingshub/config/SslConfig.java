package io.thingshub.config;

import lombok.Data;

/**
 * <p>
 * 绑定的服务器SSL配置
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Data
public class SslConfig {

	private String crt;

	private String key;

	private String ca;

}
