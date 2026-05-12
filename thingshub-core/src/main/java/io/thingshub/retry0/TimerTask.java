package io.thingshub.retry0;

import lombok.Getter;

class TimerTask {

	@Getter
	private long taskId;

	@Getter
	private Runnable command;

	@Getter
	private long startAt; // ms

	public TimerTask(long taskId, Runnable command, long startAt) {
		this.taskId = taskId;
		this.command = command;
		this.startAt = startAt;
	}

}