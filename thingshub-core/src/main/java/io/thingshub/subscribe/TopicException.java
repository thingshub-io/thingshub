package io.thingshub.subscribe;

import io.thingshub.commons.SysException;

public class TopicException extends SysException {

	private static final long serialVersionUID = 1L;

	public TopicException(final String message) {
		super(message);
	}
}