package io.thingshub.monitor;

import java.util.Optional;

import lombok.Getter;

@Getter
public class MetricManagerHolder {

	public static MeterManager metricManager = null;

	public static MeterManager setMetricManager(MeterManager metricManager) {
		MetricManagerHolder.metricManager = metricManager;
		return metricManager;
	}

	public static Optional<MetricRegistry> getMetricRegistry() {
		return Optional.of(metricManager).map(MeterManager::getMetricRegistry);
	}

	public static MeterManager getMetricManager() {
		return metricManager;
	}

}
