package io.thingshub.script;

import io.thingshub.commons.SysException;

public class ScriptException extends SysException {

	private static final long serialVersionUID = 1L;

	public ScriptException(String message) {
		super(message);
	}

	public ScriptException(String message, Throwable t) {
		super(message, t);
	}
}
