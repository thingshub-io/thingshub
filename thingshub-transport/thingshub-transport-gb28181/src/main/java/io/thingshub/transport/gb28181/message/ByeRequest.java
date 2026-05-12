package io.thingshub.transport.gb28181.message;

import javax.sip.message.Request;

import gov.nist.javax.sip.message.SIPMessage;
import io.thingshub.transport.TransportPacket;
import lombok.Getter;

@Getter
public class ByeRequest extends TransportPacket {

	private SIPMessage sipMessage;

	public ByeRequest(SIPMessage sipMessage) {
		this.packetId = 0;
		this.packetType = 7;
		this.packetName = Request.BYE;
		this.timestamp = System.currentTimeMillis();

		this.sipMessage = sipMessage;
	}

}
