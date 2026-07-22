package io.thingshub.transport.gb28181.processor;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;

public interface SipRequestProcessor {

	String getMethod();

	void process(RequestEvent event);

	default void process(RequestEvent event, ServerTransaction serverTransaction) {
		process(event);
	}

}