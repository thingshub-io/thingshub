package io.thingshub.dashboard.controller;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import io.thingshub.commons.IdParam;
import io.thingshub.commons.ItemOption;
import io.thingshub.commons.LongId;
import io.thingshub.commons.ServiceException;
import io.thingshub.commons.model.Page;
import io.thingshub.dashboard.params.DeviceRequestParams.DeviceFormParams;
import io.thingshub.dashboard.params.DeviceRequestParams.DeviceGroupFormParams;
import io.thingshub.dashboard.params.DeviceRequestParams.PublishParams;
import io.thingshub.dashboard.params.DeviceRequestParams.QueryDeviceGroupParams;
import io.thingshub.dashboard.params.DeviceRequestParams.QueryDeviceParams;
import io.thingshub.dashboard.server.UserContextHolder;
import io.thingshub.dashboard.server.UserInfo;
import io.thingshub.entity.Device;
import io.thingshub.entity.DeviceGroup;
import io.thingshub.entity.Product;
import io.thingshub.http.HttpMethod;
import io.thingshub.http.annotation.Controller;
import io.thingshub.http.annotation.RequestBody;
import io.thingshub.http.annotation.RequestMapping;
import io.thingshub.http.annotation.RequestParam;
import io.thingshub.service.DeviceGroupService;
import io.thingshub.service.DeviceService;
import io.thingshub.service.ProductService;
import io.thingshub.service.base.BaseService.AvailableStatus;
import io.thingshub.service.base.BaseService.DeletedStatus;
import jakarta.inject.Inject;

/**
 * <p>
 * 设备管理
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Controller
public class DeviceController {

	@Inject
	private ProductService productService;

	@Inject
	private DeviceService deviceService;

	@Inject
	private DeviceGroupService deviceGroupService;

	@RequestMapping(path = "/device/group/options")
	public List<ItemOption> listDeviceGroupOptions() {
		List<ItemOption> deviceGroupOptions = Collections.emptyList();

		List<DeviceGroup> deviceGroups = deviceGroupService.list();
		if (deviceGroups != null) {
			deviceGroupOptions = deviceGroups.stream().map(grp -> new ItemOption(grp.getId() + "", grp.getName())).collect(Collectors.toList());

			deviceGroupOptions.add(0, new ItemOption("0", "默认"));
		}

		return deviceGroupOptions;
	}

	@RequestMapping(path = "/device/group/query")
	public Page<DeviceGroup> queryDeviceGroup(QueryDeviceGroupParams params) {
		Map<String, Object> queryParams = new HashMap<>();

		return deviceGroupService.queryDeviceGroups(queryParams, params.getPage(), params.getSize());
	}

	@RequestMapping(path = "/device/group/info")
	public DeviceGroup getDeviceGroup(@RequestParam Long id) {
		return deviceGroupService.getByKey(id);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/device/group/save")
	public LongId saveDeviceGroup(@RequestBody DeviceGroupFormParams params) throws ServiceException {
		UserInfo userInfo = UserContextHolder.getCurrentUser();
		Long groupId = params.getId();
		if (groupId == null) {
			DeviceGroup deviceGroup = new DeviceGroup();
			deviceGroup.setName(params.getName());
			deviceGroup.setRemark(params.getRemark());
			deviceGroup.setDeletedStatus(DeletedStatus.NOT_DELETED.value());
			deviceGroup.setCreateBy(userInfo.getName());
			deviceGroup.setCreateTime(new Date());
			groupId = deviceGroupService.createDeviceGroup(deviceGroup);
		} else {
			DeviceGroup deviceGroup = deviceGroupService.getByKey(params.getId());
			if (deviceGroup == null) {
				throw new ServiceException("无效的分组ID");
			}

			deviceGroup.setName(params.getName());
			deviceGroup.setRemark(params.getRemark());
			deviceGroupService.updateDeviceGroup(deviceGroup);
		}

		return new LongId(groupId);
	}

	@RequestMapping(path = "/device/query")
	public Page<Device> queryDevcie(@RequestBody QueryDeviceParams params) {
		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("sn", params.getSn());
		return deviceService.queryDevices(queryParams, params.getPage(), params.getSize());
	}

	@RequestMapping(path = "/device/info")
	public Device getDevice(@RequestParam Long id) {
		return deviceService.getByKey(id);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/device/save")
	public LongId saveDevice(@RequestBody DeviceFormParams params) throws ServiceException {
		UserInfo userInfo = UserContextHolder.getCurrentUser();

		Product product = productService.getByCode(params.getProductCode());
		if (product == null) {
			throw new ServiceException("产品不存在");
		}

		DeviceGroup selectedDeviceGroup = null;
		if (params.getGroupId() != null) {
			if (params.getGroupId() == 0) {
				selectedDeviceGroup = new DeviceGroup();
				selectedDeviceGroup.setId(0L);
				selectedDeviceGroup.setName("默认");
			} else {
				selectedDeviceGroup = deviceGroupService.getByKey(params.getGroupId());
			}
		}

		Long deviceId = params.getId();
		if (deviceId == null) {
			Device device = new Device();
			BeanUtil.copyProperties(params, device, CopyOptions.create().setIgnoreNullValue(true));

			device.setProductName(product.getName());
			if (device.getUsername() == null || device.getPassword() == null) {
				device.setUsername(product.getAccessKey());
				device.setPassword(product.getAccessSecret());
			}

			if (selectedDeviceGroup != null) {
				device.setGroupId(selectedDeviceGroup.getId());
				device.setGroupName(selectedDeviceGroup.getName());
			}

			device.setConnectState(0);
			device.setFaultState(0);
			device.setStatus(AvailableStatus.NORMAL.value());
			device.setDeletedStatus(DeletedStatus.NOT_DELETED.value());
			device.setCreateBy(userInfo.getName());
			device.setCreateTime(new Date());
			deviceId = deviceService.createDevice(device);

			selectedDeviceGroup.setDeviceCount(selectedDeviceGroup.getDeviceCount() + 1);
			deviceGroupService.save(selectedDeviceGroup.getId(), selectedDeviceGroup);
		} else {
			Device device = deviceService.getByKey(params.getId());
			if (device == null) {
				throw new ServiceException("无效的设备ID");
			}

			BeanUtil.copyProperties(params, device, CopyOptions.create().setIgnoreNullValue(true));
			device.setProductName(product.getName());
			if (selectedDeviceGroup != null) {
				device.setGroupId(selectedDeviceGroup.getId());
				device.setGroupName(selectedDeviceGroup.getName());

				Long oldGroupId = device.getGroupId();
				if (selectedDeviceGroup.getId().longValue() != oldGroupId.longValue()) {
					selectedDeviceGroup.setDeviceCount(selectedDeviceGroup.getDeviceCount() + 1);
					deviceGroupService.save(selectedDeviceGroup.getId(), selectedDeviceGroup);

					DeviceGroup oldDeviceGroup = deviceGroupService.getByKey(oldGroupId);
					oldDeviceGroup.setDeviceCount(oldDeviceGroup.getDeviceCount() - 1);
					deviceGroupService.save(oldDeviceGroup.getId(), oldDeviceGroup);
				}
			}

			deviceService.updateDevice(device);
		}

		return new LongId(deviceId);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/device/publish")
	public void publish(@RequestBody PublishParams params) {

	}

	@RequestMapping(method = HttpMethod.POST, path = "/device/disable")
	public void disable(@RequestBody IdParam param) {
		deviceService.disable(param.getId());
	}

	@RequestMapping(method = HttpMethod.POST, path = "/device/enable")
	public void enable(@RequestBody IdParam param) {
		deviceService.enable(param.getId());
	}

	@RequestMapping(method = HttpMethod.POST, path = "/device/remove")
	public void remove(@RequestBody IdParam param) {
		deviceService.remove(param.getId());
	}

}