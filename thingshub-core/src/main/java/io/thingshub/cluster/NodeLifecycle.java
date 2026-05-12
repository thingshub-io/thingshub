package io.thingshub.cluster;

import org.apache.ignite.Ignite;
import org.apache.ignite.lifecycle.LifecycleBean;
import org.apache.ignite.lifecycle.LifecycleEventType;
import org.apache.ignite.resources.IgniteInstanceResource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeLifecycle implements LifecycleBean {

	@IgniteInstanceResource
	public Ignite ignite;

	@Override
	public void onLifecycleEvent(LifecycleEventType evt) {
		switch (evt) {
		case BEFORE_NODE_START -> log.debug("Before the node (consistentId = {}) starts, do something.", ignite.cluster().node().consistentId());
		case AFTER_NODE_START -> log.debug("After the node (consistentId = {}) starts, do something.", ignite.cluster().node().consistentId());
		case BEFORE_NODE_STOP -> log.debug("Before the node (consistentId = {}) stops, do something.", ignite.cluster().node().consistentId());
		case AFTER_NODE_STOP -> log.debug("After the node (consistentId = {}) stops, do something.", ignite.cluster().node().consistentId());
		default -> log.debug("Not supported lifecyle event type: {} on the node (consistentId = {}).", evt.name(), ignite.cluster().node().consistentId());
		}
	}
}