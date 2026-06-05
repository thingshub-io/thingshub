package io.thingshub.dashboard.controller;

import static io.thingshub.commons.ThingshubConstants.THINGSHUB_CLIENT_ID_PLACEHOLDER;
import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_EVENT_POST;
import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_EVENT_REPLY;
import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_PROPERTY_POST;
import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_PROPERTY_REPLY;
import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_SERVICE_FUNCTION_CALL;
import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_SERVICE_FUNCTION_REPLY;
import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_SERVICE_REQUEST_POST;
import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_SERVICE_REQUEST_REPLY;

import java.util.Date;
import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import io.thingshub.commons.IdParam;
import io.thingshub.commons.LongId;
import io.thingshub.commons.MessageType;
import io.thingshub.commons.ServiceException;
import io.thingshub.dashboard.params.MessageDefinitionRequestParams.MessageDefinitionFormParams;
import io.thingshub.dashboard.server.UserContextHolder;
import io.thingshub.dashboard.server.UserInfo;
import io.thingshub.entity.MessageDefinition;
import io.thingshub.http.HttpMethod;
import io.thingshub.http.annotation.Controller;
import io.thingshub.http.annotation.RequestBody;
import io.thingshub.http.annotation.RequestMapping;
import io.thingshub.http.annotation.RequestParam;
import io.thingshub.service.MessageDefinitionService;
import io.thingshub.service.base.BaseService.DeletedStatus;
import jakarta.inject.Inject;

/**
 * <p>
 * Product Message Definition API
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Controller
public class MessageDefinitionController {

	@Inject
	private MessageDefinitionService messageDefinitionService;

	@RequestMapping(path = "/product/message-definition/list")
	public List<MessageDefinition> listMessageDefinitions(@RequestParam String productCode) {
		return messageDefinitionService.getMessageDefinitions(productCode);
	}

	@RequestMapping(path = "/product/message-definition/info")
	public MessageDefinition getMessageDefinition(@RequestParam Long id) {
		return messageDefinitionService.getByKey(id);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/product/message-definition/save")
	public LongId saveMessageDefinition(@RequestBody MessageDefinitionFormParams params) throws ServiceException {
		UserInfo userInfo = UserContextHolder.getCurrentUser();

		Long messageDefinitionId = params.getId();
		if (messageDefinitionId == null) {
			if (params.getTopic() == null) {
				String targetTopic = switch (MessageType.of(params.getType())) {
				case PROPERTY_POST -> String.format(THING_TOPIC_PROPERTY_POST, params.getProductCode(), THINGSHUB_CLIENT_ID_PLACEHOLDER, params.getName());
				case PROPERTY_REPLY -> String.format(THING_TOPIC_PROPERTY_REPLY, params.getProductCode(), THINGSHUB_CLIENT_ID_PLACEHOLDER, params.getName());
				case SERVICE_FUNCTION_CALL -> String.format(THING_TOPIC_SERVICE_FUNCTION_CALL, params.getProductCode(), THINGSHUB_CLIENT_ID_PLACEHOLDER, params.getName());
				case SERVICE_FUNCTION_REPLY -> String.format(THING_TOPIC_SERVICE_FUNCTION_REPLY, params.getProductCode(), THINGSHUB_CLIENT_ID_PLACEHOLDER, params.getName());
				case SERVICE_REQUEST_POST -> String.format(THING_TOPIC_SERVICE_REQUEST_POST, params.getProductCode(), THINGSHUB_CLIENT_ID_PLACEHOLDER, params.getName());
				case SERVICE_REQUEST_REPLY -> String.format(THING_TOPIC_SERVICE_REQUEST_REPLY, params.getProductCode(), THINGSHUB_CLIENT_ID_PLACEHOLDER, params.getName());
				case EVENT_POST -> String.format(THING_TOPIC_EVENT_POST, params.getProductCode(), THINGSHUB_CLIENT_ID_PLACEHOLDER, params.getName());
				case EVENT_REPLY -> String.format(THING_TOPIC_EVENT_REPLY, params.getProductCode(), THINGSHUB_CLIENT_ID_PLACEHOLDER, params.getName());
				};

				params.setTopic(targetTopic);
			}
			MessageDefinition messageDefinition = new MessageDefinition();
			BeanUtil.copyProperties(params, messageDefinition, CopyOptions.create().setIgnoreNullValue(true));

			messageDefinition.setDeletedStatus(DeletedStatus.NOT_DELETED.value());
			messageDefinition.setCreateBy(userInfo.getName());
			messageDefinition.setCreateTime(new Date());
			messageDefinitionId = messageDefinitionService.createMessageDefinition(messageDefinition);
		} else {
			MessageDefinition messageDefinition = messageDefinitionService.getByKey(params.getId());
			if (messageDefinition == null) {
				throw new ServiceException("无效的消息ID");
			}

			BeanUtil.copyProperties(params, messageDefinition, CopyOptions.create().setIgnoreNullValue(true));
			messageDefinitionService.updateMessageDefinition(messageDefinition);
		}

		return new LongId(messageDefinitionId);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/product/message-definition/remove")
	public void remove(@RequestBody IdParam idParam) {
		messageDefinitionService.remove(idParam.getId());
	}

}