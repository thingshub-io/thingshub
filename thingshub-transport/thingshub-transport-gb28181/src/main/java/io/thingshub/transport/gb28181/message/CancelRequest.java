package io.thingshub.transport.gb28181.message;

import javax.sip.message.Request;

import gov.nist.javax.sip.message.SIPMessage;
import io.thingshub.transport.TransportPacket;
import lombok.Getter;

@Getter
public class CancelRequest extends TransportPacket {

	private SIPMessage sipMessage;

	public CancelRequest(SIPMessage sipMessage) {
		this.packetId = 0;
		this.packetType = 4;
		this.packetName = Request.CANCEL;
		this.timestamp = System.currentTimeMillis();

		this.sipMessage = sipMessage;
	}

}
