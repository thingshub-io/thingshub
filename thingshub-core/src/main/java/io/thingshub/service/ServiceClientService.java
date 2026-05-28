package io.thingshub.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;

import cn.hutool.db.sql.Condition;
import io.thingshub.commons.ServiceException;
import io.thingshub.commons.model.Page;
import io.thingshub.entity.ServiceClient;
import io.thingshub.ioc.Service;
import io.thingshub.service.base.BaseService;
import io.thingshub.service.base.IdGenerator;
import jakarta.inject.Inject;

/**
 * <p>
 * Service Client Management
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class ServiceClientService extends BaseService<Long, ServiceClient> {

	@Inject
	private IdGenerator idGenerator;

	public ServiceClient getServiceClient(Long id) {
		ServiceClient serviceClient = this.getByKey(id);
		if (serviceClient != null && serviceClient.getDeletedStatus() != DeletedStatus.DELETED.value()) {
			return serviceClient;
		} else {
			return null;
		}
	}

	public ServiceClient getServiceClient(String username) {
		return this.getOne(Lists.newArrayList(new Condition("username", username), new Condition("deleted_status", DeletedStatus.NOT_DELETED.value())));
	}

	public Page<ServiceClient> queryServiceClients(Map<String, Object> params, int page, int size) {
		List<Condition> conditions = params.entrySet().stream().map(entry -> {
			// TODO key到col 名称的映射
			return new Condition(entry.getKey(), entry.getValue());
		}).collect(Collectors.toList());
		conditions.add(new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));

		return this.query(conditions, page, size);
	}

	public Long createServiceClient(ServiceClient serviceClient) throws ServiceException {
		List<Condition> queryConditions = Lists.newArrayList(new Condition("username", serviceClient.getUsername()),
				new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));
		if (this.getOne(queryConditions) != null) {
			throw new ServiceException("用户名已存在");
		}

		long id = idGenerator.nextId();
		serviceClient.setId(id);
		this.save(id, serviceClient);

		return id;
	}

	public void updateServiceClient(ServiceClient serviceClient) throws ServiceException {
		List<Condition> queryConditions = Lists.newArrayList(new Condition("username", serviceClient.getUsername()),
				new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));
		ServiceClient theServiceClient = this.getOne(queryConditions);
		if (theServiceClient != null && !theServiceClient.getId().equals(serviceClient.getId())) {
			throw new ServiceException("用户名已存在");
		}

		this.save(serviceClient.getId(), serviceClient);
	}

	public void updateStatus(Long id, AvailableStatus availbaleStatus) {
		ServiceClient serviceClient = this.getByKey(id);
		if (serviceClient != null) {
			serviceClient.setStatus(availbaleStatus.value());

			this.save(id, serviceClient);
		}
	}

	public void remove(Long id) {
		ServiceClient serviceClient = this.getByKey(id);
		if (serviceClient != null) {
			serviceClient.setDeletedStatus(DeletedStatus.DELETED.value());

			this.save(id, serviceClient);
		}
	}

	public ServiceClient bindProducts(Long id, List<String> productCodes) {
		ServiceClient serviceClient = this.getByKey(id);
		if (serviceClient != null) {
			serviceClient.setProductCodes(JSON.toJSONString(productCodes));

			this.save(id, serviceClient);

		}

		return serviceClient;
	}

}
