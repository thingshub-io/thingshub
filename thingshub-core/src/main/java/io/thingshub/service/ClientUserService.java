package io.thingshub.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;

import cn.hutool.db.sql.Condition;
import io.thingshub.commons.ServiceException;
import io.thingshub.commons.model.Page;
import io.thingshub.entity.ClientUser;
import io.thingshub.ioc.Service;
import io.thingshub.service.base.BaseService;
import io.thingshub.service.base.IdGenerator;
import jakarta.inject.Inject;

/**
 * <p>
 * Client User Management
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class ClientUserService extends BaseService<Long, ClientUser> {

	@Inject
	private IdGenerator idGenerator;

	public ClientUser getClientUser(Long id) {
		ClientUser clientUser = this.getByKey(id);
		if (clientUser != null && clientUser.getDeletedStatus() != DeletedStatus.DELETED.value()) {
			return clientUser;
		} else {
			return null;
		}
	}

	public ClientUser getClientUser(String username) {
		return this.getOne(Lists.newArrayList(new Condition("username", username), new Condition("deleted_status", DeletedStatus.NOT_DELETED.value())));
	}

	public Page<ClientUser> queryClientUsers(Map<String, Object> params, int page, int size) {
		List<Condition> conditions = params.entrySet().stream().map(entry -> {
			// TODO key到col 名称的映射
			return new Condition(entry.getKey(), entry.getValue());
		}).collect(Collectors.toList());
		conditions.add(new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));

		return this.query(conditions, page, size);
	}

	public Long createClientUser(ClientUser clientUser) throws ServiceException {
		List<Condition> queryConditions = Lists.newArrayList(new Condition("username", clientUser.getUsername()), new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));
		if (this.getOne(queryConditions) != null) {
			throw new ServiceException("用户名已存在");
		}

		long id = idGenerator.nextId();
		clientUser.setId(id);
		this.save(id, clientUser);

		return id;
	}

	public void updateClientUser(ClientUser clientUser) throws ServiceException {
		List<Condition> queryConditions = Lists.newArrayList(new Condition("username", clientUser.getUsername()), new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));
		ClientUser theClientUser = this.getOne(queryConditions);
		if (theClientUser != null && !theClientUser.getId().equals(clientUser.getId())) {
			throw new ServiceException("用户名已存在");
		}

		this.save(clientUser.getId(), clientUser);
	}

	public void updateStatus(Long id, AvailableStatus availbaleStatus) {
		ClientUser clientUser = this.getByKey(id);
		if (clientUser != null) {
			clientUser.setStatus(availbaleStatus.value());

			this.save(id, clientUser);
		}
	}

	public void remove(Long id) {
		ClientUser clientUser = this.getByKey(id);
		if (clientUser != null) {
			clientUser.setDeletedStatus(DeletedStatus.DELETED.value());

			this.save(id, clientUser);
		}
	}

	public ClientUser bindProducts(Long id, List<String> productCodes) {
		ClientUser clientUser = this.getByKey(id);
		if (clientUser != null) {
			clientUser.setProductCodes(JSON.toJSONString(productCodes));

			this.save(id, clientUser);

		}

		return clientUser;
	}

}
