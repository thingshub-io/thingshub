package io.thingshub.monitor;

import lombok.Getter;

@Getter
public enum CounterType {

	CONNECT("transport.connect.count"), //
	SUBSCRIBE("transport.subscribe.count"), //

	PUBLISH_EVENT("transport.publish.event.count"), //
	CONNECT_EVENT("transport.connect.event.count"), //
	SUBSCRIBE_EVENT("transport.subscribe.event.count"), //
	UNSUBSCRIBE_EVENT("transport.unscribe.event.count"), //
	DISCONNECT_EVENT("transport.disconnect.event.count"), //
	CLOSE_EVENT("transport.close.event.count");

	private final String desc;

	CounterType(String desc) {
		this.desc = desc;
	}
}
