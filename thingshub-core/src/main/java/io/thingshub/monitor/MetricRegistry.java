package io.thingshub.monitor;

public interface MetricRegistry {

	MetricCounter getMetricCounter(CounterType counterType);

}
