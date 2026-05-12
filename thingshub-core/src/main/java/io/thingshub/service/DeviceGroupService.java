package io.thingshub.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import cn.hutool.db.sql.Condition;
import io.thingshub.commons.ServiceException;
import io.thingshub.commons.model.Page;
import io.thingshub.entity.DeviceGroup;
import io.thingshub.ioc.Service;
import io.thingshub.service.base.BaseService;
import io.thingshub.service.base.IdGenerator;
import jakarta.inject.Inject;

/**
 * <p>
 * 设备分组服务
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class DeviceGroupService extends BaseService<Long, DeviceGroup> {

	@Inject
	private IdGenerator idGenerator;

	public Page<DeviceGroup> queryDeviceGroups(Map<String, Object> params, int page, int size) {
		List<Condition> conditions = params.entrySet().stream().map(entry -> {
			// TODO key到col 名称的映射
			return new Condition(entry.getKey(), entry.getValue());
		}).collect(Collectors.toList());
		return this.query(conditions, page, size);
	}

	public Long createDeviceGroup(DeviceGroup deviceGroup) throws ServiceException {
		List<Condition> queryCondtions = Lists.newArrayList(new Condition("name", deviceGroup.getName()),
				new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));
		DeviceGroup theDeviceGroup = this.getOne(queryCondtions);
		if (theDeviceGroup != null) {
			throw new ServiceException("分组已存在");
		}

		long id = idGenerator.nextId();
		deviceGroup.setId(id);
		this.save(id, deviceGroup);

		return id;
	}

	public void updateDeviceGroup(DeviceGroup deviceGroup) throws ServiceException {
		List<Condition> queryCondtions = Lists.newArrayList(new Condition("name", deviceGroup.getName()),
				new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));
		DeviceGroup theDeviceGroup = this.getOne(queryCondtions);
		if (theDeviceGroup != null && !theDeviceGroup.getId().equals(deviceGroup.getId())) {
			throw new ServiceException("品类名称已存在");
		}

		this.save(deviceGroup.getId(), deviceGroup);
	}

}
