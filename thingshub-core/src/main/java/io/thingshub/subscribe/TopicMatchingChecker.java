package io.thingshub.subscribe;

import java.util.List;

import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskSplitAdapter;

import com.google.common.collect.Lists;

import io.thingshub.Broker;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TopicMatchingChecker extends ComputeTaskSplitAdapter<String, Boolean> {

	private static final long serialVersionUID = 6272099949235220462L;

	@Override
	public List<ComputeJob> split(int gridSize, String arg) {
		String topic = arg;
		List<ComputeJob> jobs = Lists.newArrayList(new ComputeJobAdapter() {

			private static final long serialVersionUID = -187700206485442000L;

			@Override
			public Object execute() {
				try {
					return Broker.getBean(SubscriptionManager.class).hasSubscriberInLocal(topic);
				} catch (Exception e) {
					log.error("Failed to check if the node has subscribers on topic {}. Error：", topic, e);

					return Boolean.FALSE;
				}
			}

		});

		return jobs;
	}

	@Override
	public Boolean reduce(List<ComputeJobResult> results) {
		Boolean finalResult = Boolean.FALSE;
		for (ComputeJobResult jobResult : results) {
			if ((Boolean) jobResult.getData()) {
				finalResult = Boolean.TRUE;
				break;
			}
		}

		return finalResult;
	}

}