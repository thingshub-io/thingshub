package io.thingshub.transport.gb28181.message;

import javax.sip.message.Request;

import gov.nist.javax.sip.message.SIPMessage;
import io.thingshub.transport.TransportPacket;
import lombok.Getter;

@Getter
public class InviteRequest extends TransportPacket {

	private SIPMessage sipMessage;

	public InviteRequest(SIPMessage sipMessage) {
		this.packetId = 0;
		this.packetType = 2;
		this.packetName = Request.INVITE;
		this.timestamp = System.currentTimeMillis();

		this.sipMessage = sipMessage;
	}

}
