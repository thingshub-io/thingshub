package io.thingshub.commons;

public class SysException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SysException(String msg) {
		super(msg);
	}

	public SysException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public SysException(Throwable cause) {
		super(cause);
	}

}