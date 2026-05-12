package io.thingshub.monitor;

public interface MetricCounter {

	long getCounter();

	void increment();

	void decrement();

	void reset();

	void callMeter(long counter);

	MetricBean getMetricBean();

	CounterType getCounterType();

}
