package io.thingshub.dashboard.controller;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONObject;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import io.thingshub.commons.IdParam;
import io.thingshub.commons.LongId;
import io.thingshub.commons.ServiceException;
import io.thingshub.commons.model.DataTypeSpecs;
import io.thingshub.commons.model.MessageParameter;
import io.thingshub.commons.model.MessageType;
import io.thingshub.commons.model.ThingModelType;
import io.thingshub.dashboard.event.MessageModelEvent;
import io.thingshub.dashboard.params.MessageSpecRequestParams.MessageSpecFormParams;
import io.thingshub.dashboard.server.UserContextHolder;
import io.thingshub.dashboard.server.UserInfo;
import io.thingshub.entity.MessageSpec;
import io.thingshub.event.ApplicationEventManager;
import io.thingshub.event.ApplicationEventType;
import io.thingshub.http.HttpMethod;
import io.thingshub.http.annotation.Controller;
import io.thingshub.http.annotation.RequestBody;
import io.thingshub.http.annotation.RequestMapping;
import io.thingshub.http.annotation.RequestParam;
import io.thingshub.service.MessageSpecService;
import io.thingshub.service.ThingModelService;
import io.thingshub.service.base.BaseService.DeletedStatus;
import jakarta.inject.Inject;

/**
 * <p>
 * Message Specification Management
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Controller
public class MessageSpecController {

	@Inject
	private ThingModelService thingModelService;

	@Inject
	private MessageSpecService messageSpecService;

	@Inject
	private ApplicationEventManager eventManager;

	@RequestMapping(path = "/message-spec/list")
	public List<MessageSpec> listMessageSpecs(@RequestParam String productCode) {
		return messageSpecService.getMessageSpecs(productCode);
	}

	@RequestMapping(path = "/message-spec/info")
	public MessageSpec getMessageSpec(@RequestParam Long id) {
		return messageSpecService.getByKey(id);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/message-spec/save")
	public LongId saveMessageSpec(@RequestBody MessageSpecFormParams params) throws ServiceException {
		UserInfo userInfo = UserContextHolder.getCurrentUser();

		Long messageSpecId = params.getId();
		if (messageSpecId == null) {
			MessageSpec messageSpec = new MessageSpec();
			BeanUtil.copyProperties(params, messageSpec, CopyOptions.create().setIgnoreNullValue(true));

			messageSpec.setDeletedStatus(DeletedStatus.NOT_DELETED.value());
			messageSpec.setCreateBy(userInfo.getName());
			messageSpec.setCreateTime(new Date());
			messageSpecId = messageSpecService.createMessageSpec(messageSpec);
		} else {
			MessageSpec messageSpec = messageSpecService.getByKey(params.getId());
			if (messageSpec == null) {
				throw new ServiceException("无效的消息模型ID");
			}

			BeanUtil.copyProperties(params, messageSpec, CopyOptions.create().setIgnoreNullValue(true));
			messageSpecService.updateMessageSpec(messageSpec);
		}

		List<MessageParameter> messageParameters = Collections.emptyList();
		if (params.getParameterSpecs() != null) {
			Map<String, JSONObject> parametersInThingModel = thingModelService.getParametersInThingModel(params.getProductCode());

			messageParameters = params.getParameterSpecs().stream().map(paramSpec -> {

				JSONObject theParameter = switch (ThingModelType.of(params.getCat())) {
				case PROPERTY -> parametersInThingModel.get("property_" + paramSpec.getRefId());
				case SERVICE -> {
					String serviceIdentifier = "service_" + params.getName();

					JSONObject inoutParameterInService = switch (MessageType.of(params.getType())) {
					case REQUEST -> parametersInThingModel.get(serviceIdentifier + "_input_" + paramSpec.getRefId());
					case REPLY -> parametersInThingModel.get(serviceIdentifier + "_output_" + paramSpec.getRefId());
					};

					yield inoutParameterInService;
				}
				case EVENT -> {
					String eventIdentifier = "event_" + params.getName();

					JSONObject inoutParameterInEvent = switch (MessageType.of(params.getType())) {
					case REQUEST -> parametersInThingModel.get(eventIdentifier + "_input_" + paramSpec.getRefId());
					case REPLY -> parametersInThingModel.get(eventIdentifier + "_output_" + paramSpec.getRefId());
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
					messageParameter.setRequired(paramSpec.isRequired());
					messageParameter.setDesc(theParameter.getString("desc"));
				}

				return messageParameter;
			}).filter(mp -> mp != null).toList();
		}

		MessageModelEvent event = null;
		if (messageSpecId == null) {
			event = new MessageModelEvent(this, params.getProductCode(), params.getName(), messageParameters, ApplicationEventType.CREATED.name());
		} else {
			event = new MessageModelEvent(this, params.getProductCode(), params.getName(), messageParameters, ApplicationEventType.UPDATED.name());
		}
		eventManager.publishEvent(event);

		return new LongId(messageSpecId);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/message-spec/remove")
	public void remove(@RequestBody IdParam idParam) {
		MessageSpec messageSpec = messageSpecService.getByKey(idParam.getId());
		messageSpecService.remove(idParam.getId());

		eventManager.publishEvent(new MessageModelEvent(this, messageSpec.getProductCode(), messageSpec.getName(), null, ApplicationEventType.REMOVED.name()));
	}

}