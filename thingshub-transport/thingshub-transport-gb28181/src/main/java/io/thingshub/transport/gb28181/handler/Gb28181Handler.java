package io.thingshub.transport.gb28181.handler;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.netty.codec.sip.SipMessageEvent;

@Sharable
public final class Gb28181Handler extends SimpleChannelInboundHandler<SipMessageEvent> {

	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final SipMessageEvent event) throws Exception {
		final SipMessage msg = event.getMessage();
		if (msg.isAck()) {
			return;
		}

		if (msg.isRequest()) {
			final SipResponse response = msg.createResponse(200);
			event.getConnection().send(response);
		}
	}

}