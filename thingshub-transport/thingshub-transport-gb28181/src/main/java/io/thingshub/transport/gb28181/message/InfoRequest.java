package io.thingshub.transport.gb28181.message;

import javax.sip.message.Request;

import gov.nist.javax.sip.message.SIPMessage;
import io.thingshub.transport.TransportPacket;
import lombok.Getter;

@Getter
public class InfoRequest extends TransportPacket {

	private SIPMessage sipMessage;

	public InfoRequest(SIPMessage sipMessage) {
		this.packetId = 0;
		this.packetType = 3;
		this.packetName = Request.ACK;
		this.timestamp = System.currentTimeMillis();

		this.sipMessage = sipMessage;
	}

}
