package io.thingshub.transport.gb28181.subscribe;

public interface Event {
	/**
	 * 回调
	 *
	 * @param eventResult
	 */
	void response(EventResult eventResult);
}