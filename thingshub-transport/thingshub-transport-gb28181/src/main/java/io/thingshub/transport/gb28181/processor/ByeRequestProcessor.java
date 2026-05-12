package io.thingshub.transport.gb28181.processor;

import io.thingshub.transport.Processor;
import io.thingshub.transport.gb28181.Gb28181ChannelContext;
import io.thingshub.transport.gb28181.message.ByeRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * BYE请求处理器
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j(topic = "io.thingshub.transport.processor")
public class ByeRequestProcessor implements Processor<Gb28181ChannelContext, ByeRequest> {

	@Override
	public void process(@NonNull Gb28181ChannelContext ctx, @NonNull ByeRequest request) {
		// TODO
	}

}
