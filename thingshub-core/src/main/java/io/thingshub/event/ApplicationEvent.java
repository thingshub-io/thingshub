package io.thingshub.event;

@SuppressWarnings("serial")
public abstract class ApplicationEvent implements java.io.Serializable {

	private final long timestamp;

	protected final transient Object source;

	public ApplicationEvent(Object source) {
		this.source = source;
		this.timestamp = System.currentTimeMillis();
	}

	public final long getTimestamp() {
		return this.timestamp;
	}

	public Object getSource() {
		return source;
	}

}
