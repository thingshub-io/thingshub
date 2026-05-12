package io.thingshub.event;

import java.util.Collection;
import java.util.concurrent.Executors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.AsyncEventBus;

import io.thingshub.ioc.Component;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ApplicationEventManager {

	private Multimap<Class<? extends ApplicationEvent>, ApplicationListener<? extends ApplicationEvent>> applicationListeners = ArrayListMultimap.create();

	private AsyncEventBus eventBus;

	@PostConstruct
	public void init() {
		this.eventBus = new AsyncEventBus(Executors.newCachedThreadPool());
		this.eventBus.register(new ApplicationEventSubscriber() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void accept(ApplicationEvent evt) {
				Collection<ApplicationListener<? extends ApplicationEvent>> eventListeners = applicationListeners.get(evt.getClass());
				if (eventListeners != null) {
					for (ApplicationListener<? extends ApplicationEvent> listener : eventListeners) {
						try {
							((ApplicationListener) listener).onApplicationEvent(evt);
						} catch (Exception e) {
							log.error("", e);
						}
					}
				}
			}

		});
	}

	public void publishEvent(ApplicationEvent event) {
		this.eventBus.post(event);
	}

	public void addListener(Class<? extends ApplicationEvent> clazz, ApplicationListener<? extends ApplicationEvent> applicationListener) {
		applicationListeners.put(clazz, applicationListener);
	}

}
