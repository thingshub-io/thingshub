package io.thingshub.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cn.hutool.db.sql.Condition;
import io.thingshub.commons.ServiceException;
import io.thingshub.entity.ThingModel;
import io.thingshub.ioc.Service;
import io.thingshub.service.base.BaseService;
import io.thingshub.service.base.IdGenerator;
import jakarta.inject.Inject;

/**
 * <p>
 * Thing Model Specification Management
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class ThingModelService extends BaseService<Long, ThingModel> {

	@Inject
	private IdGenerator idGenerator;

	public ThingModel getThingModel(String productCode) {
		return this.getOne(Lists.newArrayList(new Condition("product_code", productCode), new Condition("deleted_status", DeletedStatus.NOT_DELETED.value())));
	}

	public Long saveThingModel(String productCode, String specification, String operator) throws ServiceException {
		List<Condition> qryConditions = Lists.newArrayList(new Condition("product_code", productCode), new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));
		ThingModel thingModel = this.getOne(qryConditions);
		if (thingModel == null) {
			thingModel = new ThingModel();
			thingModel.setId(idGenerator.nextId());
			thingModel.setDeletedStatus(DeletedStatus.NOT_DELETED.value());
			thingModel.setCreateTime(new Date());
			thingModel.setCreateBy(operator);
		}

		thingModel.setProductCode(productCode);
		thingModel.setSpecification(specification);
		this.save(thingModel.getId(), thingModel);

		return thingModel.getId();
	}

	public Map<String, JSONObject> getParametersInThingModel(String productCode) {
		ThingModel thingModel = this.getThingModel(productCode);
		JSONObject thingModelSpecification = JSON.parseObject(thingModel.getSpecification());
		Map<String, JSONObject> parametersInThingModel = Maps.newHashMap();

		JSONArray propsInThingModel = thingModelSpecification.getJSONArray("properties");
		if (propsInThingModel != null) {
			for (int i = 0; i < propsInThingModel.size(); i++) {
				parametersInThingModel.put("property_" + propsInThingModel.getJSONObject(i).getString("identifier"), propsInThingModel.getJSONObject(i));
			}
		}
		JSONArray events = thingModelSpecification.getJSONArray("events");
		if (events != null) {
			for (int i = 0; i < events.size(); i++) {
				String eventIdentifier = "event_" + events.getJSONObject(i).getString("identifier");

				JSONArray inputParametersInEvent = events.getJSONObject(i).getJSONArray("inputParameters");
				if (inputParametersInEvent != null) {
					for (int j = 0; j < inputParametersInEvent.size(); j++) {
						parametersInThingModel.put(eventIdentifier + "_input_" + inputParametersInEvent.getJSONObject(j).getString("identifier"),
								inputParametersInEvent.getJSONObject(j));
					}
				}

				JSONArray outputParametersInEvent = events.getJSONObject(i).getJSONArray("outputParameters");
				if (outputParametersInEvent != null) {
					for (int j = 0; j < outputParametersInEvent.size(); j++) {
						parametersInThingModel.put(eventIdentifier + "_output_" + outputParametersInEvent.getJSONObject(j).getString("identifier"),
								outputParametersInEvent.getJSONObject(j));
					}
				}
			}
		}
		JSONArray services = thingModelSpecification.getJSONArray("services");
		if (services != null) {
			for (int i = 0; i < services.size(); i++) {
				String serviceIdentifier = "service_" + services.getJSONObject(i).getString("identifier");

				JSONArray inputParametersInService = services.getJSONObject(i).getJSONArray("inputParameters");
				if (inputParametersInService != null) {
					for (int j = 0; j < inputParametersInService.size(); j++) {
						parametersInThingModel.put(serviceIdentifier + "_input_" + inputParametersInService.getJSONObject(j).getString("identifier"),
								inputParametersInService.getJSONObject(j));
					}
				}

				JSONArray outputParametersInService = services.getJSONObject(i).getJSONArray("outputParameters");
				if (outputParametersInService != null) {
					for (int j = 0; j < outputParametersInService.size(); j++) {
						parametersInThingModel.put(serviceIdentifier + "_output_" + outputParametersInService.getJSONObject(j).getString("identifier"),
								outputParametersInService.getJSONObject(j));
					}
				}
			}
		}

		return parametersInThingModel;
	}

}
