package io.thingshub.transport.tcp;

import io.netty.channel.ChannelHandlerContext;
import io.thingshub.config.TenantSettings;
import io.thingshub.transport.ChannelContextWrapper;

public class TcpChannelContext extends ChannelContextWrapper {

	public TcpChannelContext(ChannelHandlerContext ctx, TenantSettings tenantSettings, String clientId, String clientAddr, int keepalive) {
		super(ctx, tenantSettings, clientId, clientAddr, keepalive, "1.0", 0);
	}

}
