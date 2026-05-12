package io.thingshub.plugin;

import io.netty.channel.ChannelHandlerContext;
import lombok.NonNull;

public interface Processor<C extends ChannelHandlerContext, M> {

	void process(@NonNull C ctx, @NonNull M message);

}
