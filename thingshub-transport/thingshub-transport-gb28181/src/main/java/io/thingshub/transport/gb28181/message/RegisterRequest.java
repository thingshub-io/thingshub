package io.thingshub.transport.gb28181.message;

import javax.sip.message.Request;

import gov.nist.javax.sip.message.SIPMessage;
import io.thingshub.transport.TransportPacket;
import lombok.Getter;

@Getter
public class RegisterRequest extends TransportPacket {

	private SIPMessage sipMessage;

	public RegisterRequest(SIPMessage sipMessage) {
		this.packetId = 0;
		this.packetType = 1;
		this.packetName = Request.REGISTER;
		this.timestamp = System.currentTimeMillis();

		this.sipMessage = sipMessage;
	}

}
