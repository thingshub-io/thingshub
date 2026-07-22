package io.thingshub.transport.gb28181.transaction;

import javax.sip.RequestEvent;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;

import lombok.Data;

@Data
public class Transaction {

	private String callId;

	private Long cSeq;

	private String method;

	private String fromHeader;

	private String toHeader;

	private String fromTag;

	private String toTag;

	private String viaBranch;

	public static Transaction from(RequestEvent requestEvent) {
		Request request = requestEvent.getRequest();
		Transaction trans = new Transaction();

		CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
		if (callIdHeader != null) {
			trans.setCallId(callIdHeader.getCallId());
		}

		CSeqHeader cSeqHeader = (CSeqHeader) request.getHeader(CSeqHeader.NAME);
		if (cSeqHeader != null) {
			trans.setCSeq(cSeqHeader.getSeqNumber());
			trans.setMethod(cSeqHeader.getMethod());
		}

		FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
		if (fromHeader != null) {
			trans.setFromHeader(fromHeader.toString());
			trans.setFromTag(fromHeader.getTag());
		}

		ToHeader toHeader = (ToHeader) request.getHeader(ToHeader.NAME);
		if (toHeader != null) {
			trans.setToHeader(toHeader.toString());
			trans.setToTag(toHeader.getTag());
		}

		return trans;
	}

	@Override
	public String toString() {
		return String.format("SipTransactionInfo{callId='%s', cSeq=%d %s, from='%s', to='%s'}", callId, cSeq, method, fromHeader, toHeader);
	}
}