package io.thingshub.event;

@FunctionalInterface
public interface ApplicationListener<E extends ApplicationEvent> {

	void onApplicationEvent(E event);

}