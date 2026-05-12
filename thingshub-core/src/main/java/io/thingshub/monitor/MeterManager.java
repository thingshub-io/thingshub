package io.thingshub.monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import io.thingshub.transport.utils.ByteFormatUtils;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.util.Util;

public interface MeterManager {

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	MetricRegistry getMetricRegistry();

	MetricBean getMetricBean();

	default Map<String, Object> getJvmMetric() {
		Properties props = System.getProperties();
		MemoryMXBean mxb = ManagementFactory.getMemoryMXBean();
		ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
		RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

		Map<String, Object> metrics = new HashMap<>();
		metrics.put("start_time", sdf.format(new Date(runtimeBean.getStartTime())));
		metrics.put("jdk_home", props.getProperty("java.home"));
		metrics.put("jdk_version", props.getProperty("java.version"));
		metrics.put("live_thread_count", threadBean.getThreadCount());
		metrics.put("heap_max", ByteFormatUtils.formatByte(mxb.getHeapMemoryUsage().getMax()));
		metrics.put("heap_init", ByteFormatUtils.formatByte(mxb.getHeapMemoryUsage().getInit()));
		metrics.put("heap_commit", ByteFormatUtils.formatByte(mxb.getHeapMemoryUsage().getCommitted()));
		metrics.put("heap_used", ByteFormatUtils.formatByte(mxb.getHeapMemoryUsage().getUsed()));
		metrics.put("noheap_max", ByteFormatUtils.formatByte(mxb.getNonHeapMemoryUsage().getMax()));
		metrics.put("noheap_init", ByteFormatUtils.formatByte(mxb.getNonHeapMemoryUsage().getInit()));
		metrics.put("noheap_commit", ByteFormatUtils.formatByte(mxb.getNonHeapMemoryUsage().getCommitted()));
		metrics.put("noheap_used", ByteFormatUtils.formatByte(mxb.getNonHeapMemoryUsage().getUsed()));

		return metrics;
	}

	SystemInfo systemInfo = new SystemInfo();

	int OSHI_WAIT_SECOND = 500;

	default Map<String, Object> getCpuMetric() {
		HardwareAbstractionLayer hardware = systemInfo.getHardware();
		CentralProcessor processor = hardware.getProcessor();

		long[] prevTicks = processor.getSystemCpuLoadTicks();
		Util.sleep(OSHI_WAIT_SECOND);
		long[] ticks = processor.getSystemCpuLoadTicks();

		long nice = ticks[CentralProcessor.TickType.NICE.getIndex()] - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
		long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()] - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
		long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()] - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
		long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()] - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
		long cSys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()] - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
		long user = ticks[CentralProcessor.TickType.USER.getIndex()] - prevTicks[CentralProcessor.TickType.USER.getIndex()];
		long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()] - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
		long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()] - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
		long totalCpu = user + nice + cSys + idle + iowait + irq + softirq + steal;

		Map<String, Object> metrics = new HashMap<>();
		metrics.put("cpu_count", processor.getLogicalProcessorCount());
		metrics.put("cSys", new DecimalFormat("#.##%").format(cSys * 1.0 / totalCpu));
		metrics.put("user", new DecimalFormat("#.##%").format(user * 1.0 / totalCpu));
		metrics.put("iowait", new DecimalFormat("#.##%").format(iowait * 1.0 / totalCpu));
		metrics.put("idle", new DecimalFormat("#.##%").format(1.0 - (idle * 1.0 / totalCpu)));

		return metrics;
	}

	default Map<String, Object> getCounters() {
		Map<String, Object> metrics = new HashMap<>();
		metrics.put("all_connect_size", getMetricRegistry().getMetricCounter(CounterType.CONNECT).getCounter());
//		metrics.put("all_subscribe_size", getMetricRegistry().getMetricCounter(CounterType.SUBSCRIBE).getCounter());
		metrics.put("connect_size", getMetricRegistry().getMetricCounter(CounterType.CONNECT_EVENT).getCounter());
//		metrics.put("subscribe_size", getMetricRegistry().getMetricCounter(CounterType.SUBSCRIBE_EVENT).getCounter());
//		metrics.put("publish_size", getMetricRegistry().getMetricCounter(CounterType.PUBLISH_EVENT).getCounter());
		metrics.put("disconnect_size", getMetricRegistry().getMetricCounter(CounterType.DISCONNECT_EVENT).getCounter());
//		metrics.put("unsubscribe_size", getMetricRegistry().getMetricCounter(CounterType.UNSUBSCRIBE_EVENT).getCounter());
		metrics.put("close_size", getMetricRegistry().getMetricCounter(CounterType.CLOSE_EVENT).getCounter());
//		TrafficCounter trafficCounter = ContextHolder.getReceiveContext().getTrafficHandlerLoader().get().trafficCounter();
//		metrics.put("read_size", ByteFormatUtils.formatByte(trafficCounter.cumulativeReadBytes()));
//		metrics.put("read_second_size", ByteFormatUtils.formatByte(trafficCounter.currentReadBytes()));
//		metrics.put("write_size", ByteFormatUtils.formatByte(trafficCounter.cumulativeWrittenBytes()));
//		metrics.put("write_second_size", ByteFormatUtils.formatByte(trafficCounter.currentWrittenBytes()));
		return metrics;
	}

	default Map<String, Object> getEventMetric() {
		Map<String, Object> metrics = new HashMap<>();
		metrics.put("connect_size", getMetricRegistry().getMetricCounter(CounterType.CONNECT_EVENT).getCounter());
//		metrics.put("subscribe_size", getMetricRegistry().getMetricCounter(CounterType.SUBSCRIBE_EVENT).getCounter());
//		metrics.put("publish_size", getMetricRegistry().getMetricCounter(CounterType.PUBLISH_EVENT).getCounter());
		metrics.put("disconnect_size", getMetricRegistry().getMetricCounter(CounterType.DISCONNECT_EVENT).getCounter());
//		metrics.put("unsubscribe_size", getMetricRegistry().getMetricCounter(CounterType.UNSUBSCRIBE_EVENT).getCounter());
		metrics.put("close_size", getMetricRegistry().getMetricCounter(CounterType.CLOSE_EVENT).getCounter());
		return metrics;
	}

	default String scrape() {
//		if (getMetricBean().getMeterRegistry() instanceof PrometheusMeterRegistry) {
//			return ((PrometheusMeterRegistry) getMetricBean().getMeterRegistry()).scrape(TextFormat.CONTENT_TYPE_OPENMETRICS_100);
//		} else {
		return null;
//		}
	}

	default List<MetricCounter> createMetricRegistry(MetricBean metricBean) {
		List<MetricCounter> metricCounters = new ArrayList<>();
		metricCounters.add(new EventCounter(metricBean, CounterType.CONNECT_EVENT));
//		metricCounters.add(new EventCounter(metricBean, CounterType.PUBLISH_EVENT));
//		metricCounters.add(new EventCounter(metricBean, CounterType.SUBSCRIBE_EVENT));
//		metricCounters.add(new EventCounter(metricBean, CounterType.UNSUBSCRIBE_EVENT));
		metricCounters.add(new EventCounter(metricBean, CounterType.DISCONNECT_EVENT));
		metricCounters.add(new EventCounter(metricBean, CounterType.CLOSE_EVENT));
		metricCounters.add(new TotalCounter(metricBean, CounterType.CONNECT));
//		metricCounters.add(new TotalCounter(metricBean, CounterType.SUBSCRIBE));

		return metricCounters;
	}

}
