package io.thingshub.cluster.compute;

import org.apache.ignite.lang.IgniteRunnable;

public interface RunnableTask extends IgniteRunnable {

	Object[] getParams();

	Boolean isBroadcasted();

}
