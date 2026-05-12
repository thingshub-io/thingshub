package io.thingshub.retry0;

public class RejectionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RejectionException(String msg) {
		super(msg);
	}

}