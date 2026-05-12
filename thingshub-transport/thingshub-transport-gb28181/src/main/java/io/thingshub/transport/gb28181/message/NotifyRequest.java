package io.thingshub.transport.gb28181.message;

import javax.sip.message.Request;

import gov.nist.javax.sip.message.SIPMessage;
import io.thingshub.transport.TransportPacket;
import lombok.Getter;

@Getter
public class NotifyRequest extends TransportPacket {

	private SIPMessage sipMessage;

	public NotifyRequest(SIPMessage sipMessage) {
		this.packetId = 0;
		this.packetType = 5;
		this.packetName = Request.NOTIFY;
		this.timestamp = System.currentTimeMillis();

		this.sipMessage = sipMessage;
	}

}
