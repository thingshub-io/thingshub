package io.thingshub.transport.gb28181.subscribe;

import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;

import gov.nist.javax.sip.message.SIPRequest;
import lombok.Data;

@Data
public class EventResult<T> {

	public int statusCode;

	public EventType eventType;

	public String msg;

	public String callId;

	public Dialog dialog;

	public T event;

	public EventResult() {
	}

	public EventResult(T event) {
		this.event = event;
		if (event instanceof ResponseEvent responseEvent) {
			Response response = responseEvent.getResponse();
			this.eventType = EventType.RESPONSE;
			if (response != null) {
				this.msg = response.getReasonPhrase();
				this.statusCode = response.getStatusCode();
			}
			assert response != null;
			this.callId = ((CallIdHeader) response.getHeader(CallIdHeader.NAME)).getCallId();
		} else if (event instanceof TimeoutEvent timeoutEvent) {
			this.eventType = EventType.TIMEOUT;
			this.msg = "消息超时未回复";
			this.statusCode = -1024;
			if (timeoutEvent.isServerTransaction()) {
				this.callId = ((SIPRequest) timeoutEvent.getServerTransaction().getRequest()).getCallIdHeader().getCallId();
				this.dialog = timeoutEvent.getServerTransaction().getDialog();
			} else {
				this.callId = ((SIPRequest) timeoutEvent.getClientTransaction().getRequest()).getCallIdHeader().getCallId();
				this.dialog = timeoutEvent.getClientTransaction().getDialog();
			}
		} else if (event instanceof TransactionTerminatedEvent transactionTerminatedEvent) {
			this.eventType = EventType.TRANSACTIONTERMINATED;
			this.msg = "事务已结束";
			this.statusCode = -1024;
			if (transactionTerminatedEvent.isServerTransaction()) {
				this.callId = ((SIPRequest) transactionTerminatedEvent.getServerTransaction().getRequest()).getCallIdHeader().getCallId();
				this.dialog = transactionTerminatedEvent.getServerTransaction().getDialog();
			} else {
				this.callId = ((SIPRequest) transactionTerminatedEvent.getClientTransaction().getRequest()).getCallIdHeader().getCallId();
				this.dialog = transactionTerminatedEvent.getClientTransaction().getDialog();
				this.dialog = transactionTerminatedEvent.getClientTransaction().getDialog();
			}
		} else if (event instanceof DialogTerminatedEvent dialogTerminatedEvent) {
			this.eventType = EventType.DIALOGTERMINATED;
			this.msg = "会话已结束";
			this.statusCode = -1024;
			this.callId = dialogTerminatedEvent.getDialog().getCallId().getCallId();
			this.dialog = dialogTerminatedEvent.getDialog();

		} else if (event instanceof RequestEvent requestEvent) {
			this.eventType = EventType.ACK;
			this.msg = "ack event";
			this.callId = requestEvent.getDialog().getCallId().getCallId();
			this.dialog = requestEvent.getDialog();
		}
//		else if (event instanceof DeviceNotFoundEvent) {
//			this.type = EventResultType.deviceNotFoundEvent;
//			this.msg = "设备未找到";
//			this.statusCode = -1024;
//			this.callId = ((DeviceNotFoundEvent) event).getCallId();
//		}
	}
}