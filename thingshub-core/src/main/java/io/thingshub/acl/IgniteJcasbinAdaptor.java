package io.thingshub.acl;

import java.util.ArrayList;
import java.util.List;

import javax.cache.Cache;

import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.jcasbin.persist.Helper;
import org.casbin.jcasbin.util.Util;

/**
 * <p>
 * jcasbin的Ignite适配
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public class IgniteJcasbinAdaptor implements Adapter {

	private Cache<String, Object> policyModels;

	private Object object = new Object();

	public IgniteJcasbinAdaptor(Cache<String, Object> policyModels) {
		this.policyModels = policyModels;

	}

	@Override
	public void loadPolicy(Model model) {
		try {
			this.loadPolicyData(model, Helper::loadPolicyLine);
		} catch (Throwable t) {
			throw t;
		}

	}

	@Override
	public void savePolicy(Model model) {
		List<String> policy = new ArrayList<>();
		policy.addAll(this.getModelPolicy(model, "p"));
		policy.addAll(this.getModelPolicy(model, "g"));
		this.savePolicyFile(String.join("\n", policy));
	}

	private List<String> getModelPolicy(Model model, String ptype) {
		List<String> policy = new ArrayList<>();
		model.model.get(ptype).forEach((k, v) -> {
			List<String> p = v.policy.stream().map(x -> k + ", " + Util.arrayToString(x)).toList();
			policy.addAll(p);
		});
		return policy;
	}

	private void loadPolicyData(Model model, Helper.loadPolicyLineHandler<String, Model> handler) {
		policyModels.forEach((x) -> {
			handler.accept(x.getKey(), model);
		});
	}

	private void savePolicyFile(String text) {
		policyModels.put(text, object);
	}

	@Override
	public void addPolicy(String sec, String ptype, List<String> rule) {
		String key = ptype + ", " + Util.arrayToString(rule);
		policyModels.put(key, object);
	}

	@Override
	public void removePolicy(String sec, String ptype, List<String> rule) {
		String key = ptype + ", " + Util.arrayToString(rule);
		policyModels.remove(key);
	}

	@Override
	public void removeFilteredPolicy(String sec, String ptype, int fieldIndex, String... fieldValues) {
		throw new UnsupportedOperationException("not implemented");
	}
}
