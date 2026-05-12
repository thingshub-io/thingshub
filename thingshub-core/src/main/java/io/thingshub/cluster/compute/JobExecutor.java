package io.thingshub.cluster.compute;

import java.util.Collection;
import java.util.Collections;

import org.apache.ignite.Ignite;
import org.apache.ignite.cluster.ClusterGroup;

/**
 * <p>
 * Job Executor
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public class JobExecutor {

	public Ignite ignite;

	public JobExecutor(Ignite ignite) {
		this.ignite = ignite;
	}

	public void execute(RunnableTask job, boolean includeLocal) {
		ClusterGroup clusterGroup = includeLocal ? ignite.cluster() : ignite.cluster().forRemotes();
		if (clusterGroup.nodes() == null || clusterGroup.nodes().isEmpty()) {
			return;
		}

		if (job.isBroadcasted()) {
			ignite.compute(clusterGroup).broadcast(job);
		} else {
			ignite.compute(clusterGroup).run(job);
		}
	}

	public void executeAsync(RunnableTask job, boolean includeLocal) {
		ClusterGroup clusterGroup = includeLocal ? ignite.cluster() : ignite.cluster().forRemotes();
		if (clusterGroup.nodes() == null || clusterGroup.nodes().isEmpty()) {
			return;
		}

		if (job.isBroadcasted()) {
			ignite.compute(clusterGroup).broadcastAsync(job);
		} else {
			ignite.compute(clusterGroup).runAsync(job);
		}
	}

	public <R> Collection<R> execute(CallableJob<R> job, boolean includeLocal) {
		ClusterGroup clusterGroup = includeLocal ? ignite.cluster() : ignite.cluster().forRemotes();
		if (clusterGroup.nodes() == null || clusterGroup.nodes().isEmpty()) {
			return null;
		}

		if (job.isBroadcasted()) {
			return ignite.compute(clusterGroup).broadcast(job);
		} else {
			R r = ignite.compute(clusterGroup).call(job);

			return r != null ? Collections.singletonList(r) : null;
		}
	}

	public <INPUT, OUTPUT> Collection<OUTPUT> execute(ClosureJob<INPUT, OUTPUT> job, INPUT input, boolean includeLocal) {
		ClusterGroup clusterGroup = includeLocal ? ignite.cluster() : ignite.cluster().forRemotes();
		if (clusterGroup.nodes() == null || clusterGroup.nodes().isEmpty()) {
			return null;
		}

		if (job.isBroadcasted()) {
			return ignite.compute(clusterGroup).broadcast(job, input);
		} else {
			OUTPUT output = ignite.compute(clusterGroup).apply(job, input);

			return output != null ? Collections.singletonList(output) : null;
		}
	}

}
