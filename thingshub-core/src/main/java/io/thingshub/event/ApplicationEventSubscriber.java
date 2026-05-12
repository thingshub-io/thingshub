package io.thingshub.event;

import com.google.common.eventbus.Subscribe;

public interface ApplicationEventSubscriber {

	@Subscribe
	void accept(ApplicationEvent event);

}