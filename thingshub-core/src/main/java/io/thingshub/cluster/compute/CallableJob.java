package io.thingshub.cluster.compute;

import org.apache.ignite.lang.IgniteCallable;

public interface CallableJob<V> extends IgniteCallable<V> {

	String getName();

	Boolean isBroadcasted();

}
