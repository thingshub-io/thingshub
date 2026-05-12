package io.thingshub.transport.gb28181.codec;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.impl.SipParser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SipMessage1Encoder extends MessageToByteEncoder<SipMessage> {

	public static final SipMessage1Encoder INSTANCE = new SipMessage1Encoder();

	@Override
	protected void encode(final ChannelHandlerContext ctx, final SipMessage msg, final ByteBuf out) {
		try {
			final Buffer b = msg.toBuffer();
			for (int i = 0; i < b.getReadableBytes(); ++i) {
				out.writeByte(b.getByte(i));
			}
			out.writeByte(SipParser.CR);
			out.writeByte(SipParser.LF);
		} catch (final IOException e) {
			log.error("", e);
		}
	}

}