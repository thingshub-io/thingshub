package io.thingshub.acl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.cache.Cache;

import org.apache.ignite.Ignite;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.PartitionLossPolicy;
import org.apache.ignite.configuration.CacheConfiguration;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;

import io.thingshub.ioc.Component;
import io.thingshub.service.base.BaseService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 访问权限管理
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
@Component
public class AclManager {

	private final String REQUEST_SUBJECT_TEMPLATE = "%s";

	@Inject
	private Ignite ignite;

	private Enforcer enforcer;

	@PostConstruct
	public void init() {
		Model model = new Model();
		model.addDef("r", "r", "sub, obj, act");
		model.addDef("p", "p", " sub, obj, act, eft");
		model.addDef("g", "g", "_, _");
		model.addDef("e", "e", "some(where (p.eft == allow)) && !some(where (p.eft == deny))");
		model.addDef("m", "m", "r.act == p.act && keyMatch(r.obj,p.obj)  && filter(r.sub, p.sub)");

		CacheMode cacheMode = BaseService.DEFAULT_DATA_REGION.local() ? CacheMode.REPLICATED : CacheMode.PARTITIONED;
		CacheConfiguration<String, Object> configuration = new CacheConfiguration<String, Object>() //
				.setSqlSchema("THINGSHUB") //
				.setName("SysAcl") //
				.setCacheMode(cacheMode) //
				.setDataRegionName(BaseService.DEFAULT_DATA_REGION.name()) //
				.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL) //
				.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC) //
				.setBackups(1) //
				.setEagerTtl(true) //
				.setPartitionLossPolicy(PartitionLossPolicy.READ_ONLY_SAFE) //
				.setIndexedTypes(new Class[] {}) // TODO
//				.setExpiryPolicyFactory(factory)
				.setRebalanceMode(CacheRebalanceMode.ASYNC);
		Cache<String, Object> aclCache = ignite.getOrCreateCache(configuration);
		enforcer = new Enforcer(model, new IgniteJcasbinAdaptor(aclCache));
		enforcer.addFunction("filter", new AclFunction());

		add("all", "*", AclAction.PUBLISH, AclType.ALLOW);
		add("all", "*", AclAction.SUBSCRIBE, AclType.ALLOW);
		add("all", "*", AclAction.UNSUBSCRIBE, AclType.ALLOW);
	}

	public boolean check(String clientId, String source, AclAction action) {
		try {
			String subject = String.format(REQUEST_SUBJECT_TEMPLATE, clientId);
			return Optional.ofNullable(enforcer).map(ef -> ef.enforce(subject, source, action.name())).orElse(true);
		} catch (Exception e) {
			log.error("acl check error", e);
		}

		return true;
	}

	public boolean add(String sub, String source, AclAction action, AclType type) {
		return Optional.ofNullable(enforcer).map(ef -> enforcer.addNamedPolicy("p", sub, source, action.name(), type.name().toLowerCase())).orElse(true);
	}

	public boolean delete(String sub, String source, AclAction action, AclType type) {
		return Optional.ofNullable(enforcer).map(ef -> enforcer.removeNamedPolicy("p", sub, source, action.name(), type.name().toLowerCase())).orElse(true);
	}

	public List<List<String>> get(PolicyModel policyModel) {
		return Optional.ofNullable(enforcer)
				.map(ef -> enforcer.getFilteredNamedPolicy("p", 0, policyModel.subject(), policyModel.source(),
						policyModel.action() == null || AclAction.ALL == policyModel.action() ? "" : policyModel.action().name(),
						policyModel.aclType() == null || AclType.ALL == policyModel.aclType() ? "" : policyModel.aclType().name()))
				.orElse(Collections.emptyList());
	}

}
