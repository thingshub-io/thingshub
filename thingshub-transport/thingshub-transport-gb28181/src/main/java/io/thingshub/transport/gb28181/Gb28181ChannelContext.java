package io.thingshub.transport.gb28181;

import io.netty.channel.ChannelHandlerContext;
import io.thingshub.config.TenantSettings;
import io.thingshub.transport.ChannelContextWrapper;

public class Gb28181ChannelContext extends ChannelContextWrapper {

	public Gb28181ChannelContext(ChannelHandlerContext ctx, TenantSettings tenantSettings, String clientId, String clientAddr, int keepalive) {
		super(ctx, tenantSettings, clientId, clientAddr, keepalive, "", 0);
	}

}
