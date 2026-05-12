package io.thingshub.monitor;

public class EmptyMetricCounter extends WholeCounter {

	private final static EmptyMetricCounter metricCounter = new EmptyMetricCounter();

	private EmptyMetricCounter() {
		super(null);
	}

	public static EmptyMetricCounter instance() {
		return metricCounter;
	}

	protected EmptyMetricCounter(MetricBean metricBean) {
		super(metricBean);
	}

	@Override
	public void callMeter(long counter) {

	}

	@Override
	public CounterType getCounterType() {
		return null;
	}

	@Override
	public void increment() {

	}

	@Override
	public void decrement() {

	}

	@Override
	public void reset() {

	}
}
