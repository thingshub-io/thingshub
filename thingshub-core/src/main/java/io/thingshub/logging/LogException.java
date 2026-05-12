package io.thingshub.logging;

import io.thingshub.commons.SysException;

public class LogException extends SysException {

	private static final long serialVersionUID = 8134368067262195207L;

	protected Integer code = null;

	protected String msg = null;

	public LogException(String msg) {
		super(msg);
		this.msg = msg;
	}

	public LogException(String msg, int code) {
		super(msg);
		this.code = code;
		this.msg = msg;
	}

	public LogException(Exception cause) {
		super(cause);
	}

	public LogException(String msg, Exception cause) {
		super(msg, cause);
	}

	public LogException(String msg, Exception cause, int code) {
		super(msg, cause);
		this.code = code;
		this.msg = msg;
	}

	public Integer getCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}

}