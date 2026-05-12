package io.thingshub.dashboard.event;

import java.util.List;

import io.thingshub.commons.model.MessageParameter;
import io.thingshub.event.ApplicationEvent;
import lombok.Getter;

@Getter
public class MessageModelEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1L;

	private final String productCode;

	private final String name;

	private final List<MessageParameter> parameters;

	private final String action;

	public MessageModelEvent(Object source, String productCode, String name, List<MessageParameter> parameters, String action) {
		super(source);

		this.productCode = productCode;
		this.name = name;
		this.parameters = parameters;
		this.action = action;
	}

}
