package io.thingshub.service;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import cn.hutool.db.sql.Condition;
import io.thingshub.commons.Page;
import io.thingshub.commons.ServiceException;
import io.thingshub.entity.Device;
import io.thingshub.ioc.Service;
import io.thingshub.service.base.BaseService;
import io.thingshub.service.base.IdGenerator;
import io.thingshub.transport.ConnectState;
import jakarta.inject.Inject;

/**
 * <p>
 * 设备管理
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class DeviceService extends BaseService<Long, Device> {

	@Inject
	private IdGenerator idGenerator;

	public Device getBySn(String sn) {
		return this.getOne(Lists.newArrayList(new Condition("sn", sn)));
	}

	public Device getByProductSn(String productCode, String sn) {
		return this.getOne(Lists.newArrayList(new Condition("product_code", productCode), new Condition("sn", sn)));
	}

	public Page<Device> queryDevices(Map<String, Object> params, int page, int size) {
		List<Condition> conditions = Lists.newArrayList(new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));

		conditions.addAll(params.entrySet().stream().map(entry -> {
			if (entry.getKey().equals("tenantId") && entry.getValue() != null) {
				return new Condition("tenant_id", entry.getValue());
			} else if (entry.getKey().equals("customerId") && entry.getValue() != null) {
				return new Condition("customer_id", entry.getValue());
			} else if (entry.getKey().equals("productCode") && entry.getValue() != null) {
				return new Condition("product_code", entry.getValue());
			} else if (entry.getValue() != null) {
				return new Condition(entry.getKey(), entry.getValue());
			} else {
				return null;
			}
		}).filter(c -> c != null).toList());

		return this.query(conditions, page, size);
	}

	public Long createDevice(Device device) throws ServiceException {
		List<Condition> checkConditions = Lists.newArrayList(new Condition("sn", device.getSn()), new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));
		if (this.getOne(checkConditions) != null) {
			throw new ServiceException("设备序列号已存在");
		}

		long id = idGenerator.nextId();
		device.setId(id);
		this.save(id, device);

		return id;
	}

	public void updateDevice(Device device) throws ServiceException {
		List<Condition> queryConditions = Lists.newArrayList(new Condition("sn", device.getSn()), new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));
		Device theDevice = this.getOne(queryConditions);
		if (theDevice != null && !theDevice.getId().equals(device.getId())) {
			throw new ServiceException("设备序列号已存在");
		}

		this.save(device.getId(), device);
	}

	public void disable(Long id) {
		Device device = this.getByKey(id);
		if (device != null) {
			device.setStatus(AvailableStatus.DISABLED.value());

			this.save(id, device);
		}
	}

	public void disable(String productCode, String sn) {
		List<Condition> conditions = Lists.newArrayList(new Condition("product_code", productCode), new Condition("sn", sn),
				new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));
		Device device = this.getOne(conditions);
		if (device != null) {
			device.setStatus(AvailableStatus.DISABLED.value());

			this.save(device.getId(), device);
		}
	}

	public void enable(Long id) {
		Device device = this.getByKey(id);
		if (device != null) {
			device.setStatus(AvailableStatus.NORMAL.value());

			this.save(id, device);
		}
	}

	public void enable(String productCode, String sn) {
		List<Condition> conditions = Lists.newArrayList(new Condition("product_code", productCode), new Condition("sn", sn),
				new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));
		Device device = this.getOne(conditions);
		if (device != null) {
			device.setStatus(AvailableStatus.NORMAL.value());

			this.save(device.getId(), device);
		}
	}

	public void remove(Long id) {
		Device device = this.getByKey(id);
		if (device != null) {
			device.setDeletedStatus(DeletedStatus.DELETED.value());

			this.save(id, device);
		}
	}

	public void updateConnectState(String sn, ConnectState connectState) {
		Device device = this.getOne(Lists.newArrayList(new Condition("sn", sn)));
		if (device != null) {
			device.setConnectState(connectState.value());

			this.save(device.getId(), device);
		}
	}

}
