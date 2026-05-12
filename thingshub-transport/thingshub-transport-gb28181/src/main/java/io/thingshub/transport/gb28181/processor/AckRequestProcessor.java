package io.thingshub.transport.gb28181.processor;

import io.thingshub.transport.Processor;
import io.thingshub.transport.gb28181.Gb28181ChannelContext;
import io.thingshub.transport.gb28181.message.AckRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * ACK请求处理器
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j(topic = "io.thingshub.transport.processor")
public class AckRequestProcessor implements Processor<Gb28181ChannelContext, AckRequest> {

	@Override
	public void process(@NonNull Gb28181ChannelContext ctx, @NonNull AckRequest request) {
		// TODO
	}

}
