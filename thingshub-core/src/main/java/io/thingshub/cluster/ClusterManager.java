package io.thingshub.cluster;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCluster;

import io.thingshub.Broker;
import io.thingshub.cluster.compute.CallableJob;
import io.thingshub.cluster.compute.ClosureJob;
import io.thingshub.cluster.compute.JobExecutor;
import io.thingshub.cluster.compute.RunnableTask;
import io.thingshub.ioc.Component;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 集群任务管理
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Component
@Slf4j
public class ClusterManager {

	private IgniteCluster igniteCluster;

	private JobExecutor jobExecutor;

	@Inject
	public ClusterManager(Ignite ignite) {
		this.igniteCluster = ignite.cluster();
		this.jobExecutor = new JobExecutor(ignite);
	}

	public Collection<ClusterNode> getClusterNodes() {
		return jobExecutor.execute(new CallableJob<ClusterNode>() {

			private static final long serialVersionUID = 872929868018943945L;

			@Override
			public ClusterNode call() throws Exception {
				org.apache.ignite.cluster.ClusterNode igniteClusterNode = igniteCluster.localNode();
				ClusterNode clusterNode = new ClusterNode(igniteClusterNode.consistentId().toString(),
						igniteClusterNode.addresses().stream().findAny().orElse(null), Broker.BROKER_ADDR);

				return clusterNode;
			}

			@Override
			public String getName() {
				return "cluster_nodes_task";
			}

			@Override
			public Boolean isBroadcasted() {
				return true;
			}

		}, true);
	}

	public Set<String> getOtherClusterNodes() {
		return igniteCluster.nodes().stream().filter(clusterNode -> clusterNode != igniteCluster.localNode())
				.map(clusterNode -> clusterNode.consistentId().toString()).collect(Collectors.toSet());
	}

	public String getLocalNode() {
		return igniteCluster.localNode().addresses().stream().findFirst().orElse(Broker.BROKER_ADDR);
	}

	public void executeTask(String name, RunnableTask task, boolean includeLocal) {
		log.info("Execute runnable task in cluster, name: {}, params: {}", name, task.getParams());

		jobExecutor.execute(task, includeLocal);
	}

	public void executeTaskAsync(String name, RunnableTask task, boolean includeLocal) {
		log.info("Execute runnable task in cluster asynchronously, name: {}, params: {}", name, task.getParams());

		jobExecutor.executeAsync(task, includeLocal);
	}

	public <INPUT, OUTPUT> Collection<OUTPUT> executeTask(String name, ClosureJob<INPUT, OUTPUT> task, INPUT input, boolean includeLocal) {
		log.info("Execute closure task in cluster, name: {}", name);

		return jobExecutor.execute(task, input, includeLocal);
	}

}
