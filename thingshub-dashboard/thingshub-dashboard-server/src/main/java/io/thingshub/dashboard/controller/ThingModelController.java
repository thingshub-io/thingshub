package io.thingshub.dashboard.controller;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import io.thingshub.commons.ServiceException;
import io.thingshub.dashboard.params.ThingModelRequestParams.ThingEventsParams;
import io.thingshub.dashboard.params.ThingModelRequestParams.ThingPropertiesParams;
import io.thingshub.dashboard.params.ThingModelRequestParams.ThingServicesParams;
import io.thingshub.dashboard.server.UserContextHolder;
import io.thingshub.dashboard.server.UserInfo;
import io.thingshub.entity.ThingModel;
import io.thingshub.http.HttpMethod;
import io.thingshub.http.annotation.Controller;
import io.thingshub.http.annotation.RequestBody;
import io.thingshub.http.annotation.RequestMapping;
import io.thingshub.http.annotation.RequestParam;
import io.thingshub.service.ThingModelService;
import jakarta.inject.Inject;

/**
 * <p>
 * Thing Model REST API
 *
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Controller
public class ThingModelController {

	@Inject
	private ThingModelService thingModelService;

	@RequestMapping(path = "/thing-model/info")
	public Map<String, Object> getThingModel(@RequestParam String productCode) {
		ThingModel thingModel = thingModelService.getThingModel(productCode);
		Map<String, Object> data = new HashMap<>();
		if (thingModel != null) {
			data.put("productCode", thingModel.getProductCode());
			data.put("specification", JSON.parseObject(thingModel.getSpecification()));
			data.put("createTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(thingModel.getCreateTime()));
			data.put("createBy", thingModel.getCreateBy());
		}

		return data;
	}

	@RequestMapping(method = HttpMethod.POST, path = "/thing-property/save")
	public void saveThingProperty(@RequestBody ThingPropertiesParams params) throws ServiceException {
		UserInfo userInfo = UserContextHolder.getCurrentUser();

		ThingModel thingModel = thingModelService.getThingModel(params.getProductCode());
		JSONObject specification = null;
		if (thingModel != null) {
			specification = JSON.parseObject(thingModel.getSpecification());
			specification.put("properties", params.getProperties());
		} else {
			specification = new JSONObject();
			specification.put("properties", params.getProperties());
		}

		thingModelService.saveThingModel(params.getProductCode(), JSON.toJSONString(specification), userInfo.getName());
	}

	@RequestMapping(method = HttpMethod.POST, path = "/thing-service/save")
	public void saveThingService(@RequestBody ThingServicesParams params) throws ServiceException {
		UserInfo userInfo = UserContextHolder.getCurrentUser();

		ThingModel thingModel = thingModelService.getThingModel(params.getProductCode());
		JSONObject specification = null;
		if (thingModel != null) {
			specification = JSON.parseObject(thingModel.getSpecification());
			specification.put("services", params.getServices());
		} else {
			specification = new JSONObject();
			specification.put("services", params.getServices());
		}

		thingModelService.saveThingModel(params.getProductCode(), JSON.toJSONString(specification), userInfo.getName());
	}

	@RequestMapping(method = HttpMethod.POST, path = "/thing-event/save")
	public void saveThingEvent(@RequestBody ThingEventsParams params) throws ServiceException {
		UserInfo userInfo = UserContextHolder.getCurrentUser();

		ThingModel thingModel = thingModelService.getThingModel(params.getProductCode());
		JSONObject specification = null;
		if (thingModel != null) {
			specification = JSON.parseObject(thingModel.getSpecification());
			specification.put("events", params.getEvents());
		} else {
			specification = new JSONObject();
			specification.put("events", params.getEvents());
		}

		thingModelService.saveThingModel(params.getProductCode(), JSON.toJSONString(specification), userInfo.getName());
	}

}