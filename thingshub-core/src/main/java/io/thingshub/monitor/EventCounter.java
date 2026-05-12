package io.thingshub.monitor;

import io.micrometer.core.instrument.Counter;

public class EventCounter extends WholeCounter {

	private CounterType counterType;

	private Counter counter;

	public EventCounter(MetricBean metricBean, CounterType counterType) {
		super(metricBean);
		this.counterType = counterType;
		initCount();
	}

	@Override
	public void initCount() {
		this.counter = getMetricBean().getMeterRegistry().counter(getCounterType().getDesc(), getMetricBean().getTags());
	}

	@Override
	public void callMeter(long count) {
		counter.increment();
	}

	@Override
	public CounterType getCounterType() {
		return this.counterType;
	}
}
