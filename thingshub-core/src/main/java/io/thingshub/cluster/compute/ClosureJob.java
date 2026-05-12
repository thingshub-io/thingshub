package io.thingshub.cluster.compute;

import org.apache.ignite.lang.IgniteClosure;

public interface ClosureJob<INPUT, OUTPUT> extends IgniteClosure<INPUT, OUTPUT> {

	Boolean isBroadcasted();

}
