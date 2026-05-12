package io.thingshub.transport.gb28181.codec;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.impl.SipParser;

public final class SipMessage1Decoder extends ByteToMessageDecoder {

	public static final SipMessage1Decoder INSTANCE = new SipMessage1Decoder();

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		final byte[] bytes = new byte[in.readableBytes()];
		in.getBytes(0, bytes);

		final Buffer buffer = Buffers.wrap(bytes);
		SipParser.consumeSWS(buffer);
		final SipMessage sipMessage = SipParser.frame(buffer);
		out.add(sipMessage);
	}

}