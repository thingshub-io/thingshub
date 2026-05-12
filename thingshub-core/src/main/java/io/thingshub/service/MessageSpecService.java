package io.thingshub.service;

import static io.thingshub.commons.model.MessageType.REPLY;
import static io.thingshub.commons.model.MessageType.REQUEST;
import static io.thingshub.commons.model.ThingModelType.EVENT;
import static io.thingshub.commons.model.ThingModelType.PROPERTY;
import static io.thingshub.commons.model.ThingModelType.SERVICE;
import static io.thingshub.service.base.BaseService.DeletedStatus.DELETED;
import static io.thingshub.service.base.BaseService.DeletedStatus.NOT_DELETED;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import cn.hutool.db.sql.Condition;
import io.thingshub.commons.ServiceException;
import io.thingshub.commons.model.DataTypeSpecs;
import io.thingshub.commons.model.MessageModel;
import io.thingshub.commons.model.MessageParameter;
import io.thingshub.commons.model.MessageType;
import io.thingshub.commons.model.ThingModelType;
import io.thingshub.entity.MessageSpec;
import io.thingshub.ioc.Service;
import io.thingshub.service.base.BaseService;
import io.thingshub.service.base.IdGenerator;
import jakarta.inject.Inject;

/**
 * <p>
 * Product Message Specification Management
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class MessageSpecService extends BaseService<Long, MessageSpec> {

	@Inject
	private MessageAssignService messageAssignService;

	@Inject
	private IdGenerator idGenerator;

	public List<MessageSpec> getDownMessageSpecs(String productCode) {
		List<MessageSpec> downPropertyMessageSpecifications = this.query(Lists.newArrayList( //
				new Condition("product_code", productCode), //
				new Condition("cat", PROPERTY.name()), //
				new Condition("type", REPLY.name()), //
				new Condition("deleted_status", NOT_DELETED.value())));

		List<MessageSpec> downEventMessageSpecs = this.query(Lists.newArrayList( //
				new Condition("product_code", productCode), //
				new Condition("cat", EVENT.name()), //
				new Condition("type", REPLY.name()), //
				new Condition("deleted_status", NOT_DELETED.value())));

		List<MessageSpec> downServiceMessageSpecs = this.query(Lists.newArrayList( //
				new Condition("product_code", productCode), //
				new Condition("cat", SERVICE.name()), //
				new Condition("type", REQUEST.name()), //
				new Condition("deleted_status", NOT_DELETED.value())));

		return Lists.newArrayList(Iterables.concat(downPropertyMessageSpecifications, downEventMessageSpecs, downServiceMessageSpecs));
	}

	public List<MessageSpec> getMessageSpecs(String productCode) {
		return this.query(Lists.newArrayList(new Condition("product_code", productCode), new Condition("deleted_status", NOT_DELETED.value())));
	}

	public List<MessageSpec> getMessageSpecs(String clientUserName, String productCode) {
		List<Long> msgSpecIds = messageAssignService.getMessageAssigns(clientUserName, productCode).stream().map(a -> a.getMsgSpecId()).toList();

		return this.query(Lists.newArrayList(new Condition("product_code", productCode), new Condition("deleted_status", NOT_DELETED.value()))).stream()
				.filter(s -> msgSpecIds.contains(s.getId())).toList();
	}

	public MessageSpec getMessageSpec(String name) {
		List<Condition> conditions = Lists.newArrayList(new Condition("name", name));
		return this.getOne(conditions);
	}

	public Long createMessageSpec(MessageSpec messageSpec) throws ServiceException {
		List<Condition> checkConditions = Lists.newArrayList( //
				new Condition("name", messageSpec.getName()), //
				new Condition("deleted_status", NOT_DELETED.value()));
		if (this.getOne(checkConditions) != null) {
			throw new ServiceException("消息名称已存在");
		}

		long id = idGenerator.nextId();
		messageSpec.setId(id);
		this.save(id, messageSpec);

		return id;
	}

	public void updateMessageSpec(MessageSpec messageSpec) throws ServiceException {
		List<Condition> checkConditions = Lists.newArrayList( //
				new Condition("name", messageSpec.getName()), //
				new Condition("deleted_status", NOT_DELETED.value()));
		MessageSpec theMessageSpec = this.getOne(checkConditions);
		if (theMessageSpec != null && !theMessageSpec.getId().equals(messageSpec.getId())) {
			throw new ServiceException("消息名称已存在");
		}

		this.save(messageSpec.getId(), messageSpec);
	}

	public void remove(Long id) {
		MessageSpec messageSpec = this.getByKey(id);
		if (messageSpec != null) {
			messageSpec.setDeletedStatus(DELETED.value());

			this.save(id, messageSpec);
		}
	}

	public MessageModel messageSpec2MessageModel(MessageSpec msgSpec, Map<String, JSONObject> parametersInThingModel) {
		MessageModel messageModel = new MessageModel();
		messageModel.setId(msgSpec.getId());
		messageModel.setName(msgSpec.getName());
		messageModel.setTitle(msgSpec.getTitle());
		messageModel.setProductCode(msgSpec.getProductCode());
		messageModel.setCat(msgSpec.getCat());
		messageModel.setType(msgSpec.getType());
		messageModel.setTopic(msgSpec.getTopic());
		messageModel.setDescription(msgSpec.getDescription());

		if (msgSpec.getParameterSpecs() != null) {
			JSONArray parameterSpecs = JSON.parseArray(msgSpec.getParameterSpecs());
			if (parameterSpecs.size() > 0) {
				List<MessageParameter> messageParameters = parameterSpecs.stream().map(item -> {
					JSONObject paramSpec = (JSONObject) item;

					JSONObject theParameter = switch (ThingModelType.of(msgSpec.getCat())) {
					case PROPERTY -> parametersInThingModel.get("property_" + paramSpec.getString("refId"));
					case SERVICE -> {
						String serviceIdentifier = "service_" + msgSpec.getName();

						JSONObject inoutParameterInService = switch (MessageType.of(msgSpec.getType())) {
						case REQUEST -> parametersInThingModel.get(serviceIdentifier + "_input_" + paramSpec.getString("refId"));
						case REPLY -> parametersInThingModel.get(serviceIdentifier + "_output_" + paramSpec.getString("refId"));
						};

						yield inoutParameterInService;
					}
					case EVENT -> {
						String eventIdentifier = "event_" + msgSpec.getName();

						JSONObject inoutParameterInEvent = switch (MessageType.of(msgSpec.getType())) {
						case REQUEST -> parametersInThingModel.get(eventIdentifier + "_input_" + paramSpec.getString("refId"));
						case REPLY -> parametersInThingModel.get(eventIdentifier + "_output_" + paramSpec.getString("refId"));
						};

						yield inoutParameterInEvent;
					}
					};

					MessageParameter messageParameter = null;
					if (theParameter != null) {
						messageParameter = new MessageParameter();
						messageParameter.setIdentifier(theParameter.getString("identifier"));
						messageParameter.setName(theParameter.getString("name"));
						messageParameter.setDataType(theParameter.getObject("dataType", DataTypeSpecs.class));
						messageParameter.setRequired(paramSpec.getBooleanValue("required"));
						messageParameter.setDesc(theParameter.getString("desc"));
					}

					return messageParameter;
				}).filter(mp -> mp != null).toList();

				messageModel.setParameters(messageParameters);
			}
		}

		return messageModel;
	}

}
