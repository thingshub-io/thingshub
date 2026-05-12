package io.thingshub.monitor;

import java.util.Collections;
import java.util.List;

public class LocalMeterManager implements MeterManager {

	@Override
	public MetricRegistry getMetricRegistry() {
		return new AbstractMetricRegistry(createMetricRegistry(getMetricBean())) {

			@Override
			public MetricCounter getMetricCounter(CounterType counterType) {
				return EmptyMetricCounter.instance();
			}

		};
	}

	@Override
	public MetricBean getMetricBean() {
		return new LocalMetricBean();
	}

	@Override
	public List<MetricCounter> createMetricRegistry(MetricBean metricBean) {
		return Collections.emptyList();
	}
}
