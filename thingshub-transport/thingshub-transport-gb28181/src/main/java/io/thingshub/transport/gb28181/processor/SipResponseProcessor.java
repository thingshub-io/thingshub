package io.thingshub.transport.gb28181.processor;

import javax.sip.ResponseEvent;

public interface SipResponseProcessor {

	default boolean isNeedProcess(ResponseEvent evt) {
		return true;
	}

	String getMethod();

	void process(ResponseEvent evt);

}