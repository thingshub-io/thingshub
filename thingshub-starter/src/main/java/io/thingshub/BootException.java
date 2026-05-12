package io.thingshub;

import io.thingshub.commons.SysException;

public class BootException extends SysException {

	private static final long serialVersionUID = 1L;

	public BootException(String cause, Throwable e) {
		super(cause, e);
	}

	public BootException(String cause) {
		super("cause");
	}
}
