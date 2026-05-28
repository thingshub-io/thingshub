package io.thingshub.service;

import java.util.Date;
import java.util.List;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.Lists;

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

	public JSONArray getThingProperties(String productCode) {
		ThingModel thingModel = this.getThingModel(productCode);
		JSONObject thingModelSpecification = JSON.parseObject(thingModel.getSpecification());

		return thingModelSpecification.getJSONArray("properties");
	}

	public JSONObject getThingEvent(String productCode, String eventIdentifier) {
		ThingModel thingModel = this.getThingModel(productCode);
		JSONObject thingModelSpecification = JSON.parseObject(thingModel.getSpecification());

		JSONArray events = thingModelSpecification.getJSONArray("events");
		if (events != null) {
			for (Object o : events) {
				JSONObject e = (JSONObject) o;
				if (eventIdentifier.equals(e.getString("identifier"))) {
					return e;
				}
			}
		}

		return null;
	}

	public JSONObject getThingService(String productCode, String serviceIdentifier) {
		ThingModel thingModel = this.getThingModel(productCode);
		JSONObject thingModelSpecification = JSON.parseObject(thingModel.getSpecification());

		JSONArray services = thingModelSpecification.getJSONArray("services");
		if (services != null) {
			for (Object o : services) {
				JSONObject s = (JSONObject) o;
				if (serviceIdentifier.equals(s.getString("identifier"))) {
					return s;
				}
			}
		}

		return null;
	}

}
