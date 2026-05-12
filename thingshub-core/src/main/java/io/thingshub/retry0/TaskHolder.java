package io.thingshub.retry0;

class TaskHolder {

	final TimerTask task;

	final long creationTime = System.nanoTime();

	volatile boolean cancelled = false;

	WheelTimer ownerWheel;

	int bucketIndex;

	TaskHolder(TimerTask task) {
		this.task = task;
	}
}