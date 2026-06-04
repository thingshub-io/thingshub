package io.thingshub.transport.onvif;

import io.netty.channel.ChannelHandlerContext;
import io.thingshub.config.TenantSettings;
import io.thingshub.transport.ChannelContextWrapper;

public class OnvifChannelContext extends ChannelContextWrapper {

	public OnvifChannelContext(ChannelHandlerContext ctx, TenantSettings tenantSettings, String username, String clientId, String clientAddr, int keepalive) {
		super(ctx, tenantSettings, username, clientId, clientAddr, keepalive, "", 0);
	}

}
