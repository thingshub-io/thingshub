package io.thingshub.transport.gb28181.message;

import javax.sip.message.Request;

import gov.nist.javax.sip.message.SIPMessage;
import io.thingshub.transport.TransportPacket;
import lombok.Getter;

@Getter
public class SubscribeRequest extends TransportPacket {

	private SIPMessage sipMessage;

	public SubscribeRequest(SIPMessage sipMessage) {
		this.packetId = 0;
		this.packetType = 5;
		this.packetName = Request.SUBSCRIBE;
		this.timestamp = System.currentTimeMillis();

		this.sipMessage = sipMessage;
	}

}
