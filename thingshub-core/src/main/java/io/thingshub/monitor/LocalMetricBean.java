package io.thingshub.monitor;

import io.micrometer.core.instrument.MeterRegistry;

public class LocalMetricBean implements MetricBean {

	public LocalMetricBean() {
	}

	@Override
	public MetricBean Close() {
		return this;
	}

	@Override
	public MeterRegistry getMeterRegistry() {
		return null;
	}
}
