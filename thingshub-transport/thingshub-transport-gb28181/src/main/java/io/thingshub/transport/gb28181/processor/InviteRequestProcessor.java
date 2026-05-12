package io.thingshub.transport.gb28181.processor;

import io.thingshub.transport.Processor;
import io.thingshub.transport.gb28181.Gb28181ChannelContext;
import io.thingshub.transport.gb28181.message.InviteRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * INVITE请求处理器
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j(topic = "io.thingshub.transport.processor")
public class InviteRequestProcessor implements Processor<Gb28181ChannelContext, InviteRequest> {

	@Override
	public void process(@NonNull Gb28181ChannelContext ctx, @NonNull InviteRequest request) {
		// TODO
	}

}
