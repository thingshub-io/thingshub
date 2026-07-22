package io.thingshub.transport.gb28181.processor;

import javax.sip.TimeoutEvent;

public interface SipTimeoutProcessor {

	void process(TimeoutEvent event);

}