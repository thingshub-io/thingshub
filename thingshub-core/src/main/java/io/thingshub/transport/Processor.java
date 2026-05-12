package io.thingshub.transport;

import io.netty.channel.ChannelHandlerContext;

public interface Processor<Ctx extends ChannelHandlerContext, P extends TransportPacket> {

	void process(Ctx ctx, P packet);

}
