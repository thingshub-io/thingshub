package io.thingshub.transport.gb28181.processor;

import io.thingshub.transport.Processor;
import io.thingshub.transport.gb28181.Gb28181ChannelContext;
import io.thingshub.transport.gb28181.message.NotifyRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Notify请求处理器
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j(topic = "io.thingshub.transport.processor")
public class NotifyRequestProcessor implements Processor<Gb28181ChannelContext, NotifyRequest> {

	@Override
	public void process(@NonNull Gb28181ChannelContext ctx, @NonNull NotifyRequest request) {
		// TODO
	}

}
