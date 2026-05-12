package io.thingshub.transport.throttler;

public class ORCondition implements Condition {

	private final Condition[] conditions;

	private String message;

	public static Condition or(Condition... conditions) {
		return new ORCondition(conditions);
	}

	public ORCondition(Condition... conditions) {
		this.conditions = conditions;
	}

	@Override
	public boolean meet() {
		for (Condition condition : conditions) {
			if (condition.meet()) {
				message = condition.toString();
				return true;
			}
		}
		message = "";

		return false;
	}

	@Override
	public String toString() {
		return message;
	}
}
