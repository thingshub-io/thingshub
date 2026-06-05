package io.thingshub.dashboard.event;

import io.thingshub.commons.MessageSpec;
import io.thingshub.event.ApplicationEvent;
import lombok.Getter;

@Getter
public class MessageAuthorizationEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1L;

	private final String username;

	private final String productCode;

	private final String messageName;

	private final MessageSpec messageSpec;

	private final String action;

	public MessageAuthorizationEvent(Object source, String username, String productCode, String messageName, MessageSpec messageSpec, String action) {
		super(source);

		this.username = username;
		this.productCode = productCode;
		this.messageName = messageName;
		this.messageSpec = messageSpec;
		this.action = action;
	}

}
