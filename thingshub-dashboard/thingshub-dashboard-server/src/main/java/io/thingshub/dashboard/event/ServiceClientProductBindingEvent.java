package io.thingshub.dashboard.event;

import java.util.List;

import io.thingshub.event.ApplicationEvent;
import lombok.Getter;

@Getter
public class ServiceClientProductBindingEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1L;

	private final String username;

	private final List<String> productCodes;

	private final String action;

	public ServiceClientProductBindingEvent(Object source, String username, List<String> productCodes, String action) {
		super(source);

		this.username = username;
		this.productCodes = productCodes;
		this.action = action;
	}

}
