package io.thingshub.transport.mqtt.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.thingshub.ioc.Component;
import io.thingshub.service.base.BaseService;
import io.thingshub.subscribe.PrefixSearchTree;
import jakarta.annotation.PostConstruct;

/**
 * <p>
 * Retain Message Management
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Component
public class RetainService extends BaseService<String, Retain> {

	private PrefixSearchTree<Retain> prefixSearchTree;

	@PostConstruct
	public void init() {
		this.prefixSearchTree = new PrefixSearchTree<>();

		List<Retain> retains = this.list();
		if (retains != null) {
			retains.forEach(r -> prefixSearchTree.insert(r.getStdTopic(), r, false));
		}
	}

	public void cleanRetain(String stdTopic) {
		this.removeByKey(stdTopic);
		prefixSearchTree.delete(stdTopic);
	}

	public void saveRetain(Retain retain, long duration, TimeUnit unit) {
		this.save(retain.getStdTopic(), retain, duration, unit);
		prefixSearchTree.insert(retain.getStdTopic(), retain, true);
	}

	public Retain getRetain(String stdTopic) {
		return this.getByKey(stdTopic);
	}

	public List<Retain> getRetainsByPrefix(String topicPrefix) {
		return prefixSearchTree.searchByPrefix(topicPrefix, Integer.MAX_VALUE);
	}

}