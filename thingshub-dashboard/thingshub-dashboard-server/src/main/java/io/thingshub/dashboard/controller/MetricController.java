package io.thingshub.dashboard.controller;

import java.util.Collection;
import java.util.Map;

import io.thingshub.cluster.ClusterManager;
import io.thingshub.cluster.ClusterNode;
import io.thingshub.http.annotation.Controller;
import io.thingshub.http.annotation.RequestMapping;
import io.thingshub.monitor.MetricManagerHolder;
import jakarta.inject.Inject;

/**
 * <p>
 * 数据指标
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Controller
public class MetricController {

	@Inject
	private ClusterManager clusterManager;

	@RequestMapping(path = "/metric/cluster")
	public Collection<ClusterNode> queryNodes() {
		return clusterManager.getClusterNodes();
	}

	@RequestMapping(path = "/metric/counters")
	public Map<String, Object> getCounters() {
		return MetricManagerHolder.getMetricManager().getCounters();
	}

	@RequestMapping(path = "/metric/cpu")
	public Map<String, Object> getCpuMetric() {
		return MetricManagerHolder.getMetricManager().getCpuMetric();
	}

	@RequestMapping(path = "/metric/jvm")
	public Map<String, Object> getJvmMetric() {
		return MetricManagerHolder.getMetricManager().getJvmMetric();
	}

	@RequestMapping(path = "/metric/event")
	public Map<String, Object> getEventMetric() {
		return MetricManagerHolder.getMetricManager().getEventMetric();
	}

	// 物联网平台指标：产品（品类数量和产品数量），设备数量（总数，在线，离线），各产品设备数量饼图，消息趋势图（时、月、日纬度）

}