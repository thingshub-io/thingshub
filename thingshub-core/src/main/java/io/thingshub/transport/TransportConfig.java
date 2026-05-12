package io.thingshub.transport;

import java.util.Map;

import io.netty.channel.ChannelOption;
import io.thingshub.config.SslConfig;
import io.thingshub.ioc.Value;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Transport Config
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public abstract class TransportConfig {

	public abstract String getHost();

	public abstract int getPort();

	@Getter
	@Value("${thingshub.transport.wiretap: false}")
	protected boolean wiretap;

	@Getter
	@Setter
	protected Integer bossThreads = 1;

	@Getter
	@Setter
	protected Integer workerThreads = Math.max(Runtime.getRuntime().availableProcessors() * 2, 4);

	// TODO default ChannelOptions
	@Getter
	@Setter
	protected Map<ChannelOption<?>, Object> options;

	@Getter
	@Setter
	protected Map<ChannelOption<?>, Object> childOptions;

	// TODO tenant settings
	@Getter
	@Setter
	protected int connectRateLimit = 1000;// per second

	@Getter
	@Setter
	protected int connectTimeout = 20;// second

	@Getter
	protected long writeLimit = 512 * 1024; // byte/s

	@Getter
	protected long readLimit = 512 * 1024; // byte/s

	@Getter
	protected int maxBytesInMessage = 32 * 1024;

	@Getter
	@Setter
	private SslConfig ssl;

}
