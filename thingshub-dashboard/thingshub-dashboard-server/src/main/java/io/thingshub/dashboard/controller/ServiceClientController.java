package io.thingshub.dashboard.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.Sets;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import io.thingshub.commons.IdParam;
import io.thingshub.commons.LongId;
import io.thingshub.commons.ServiceException;
import io.thingshub.commons.model.DataTypeSpecs;
import io.thingshub.commons.model.MessageParameter;
import io.thingshub.commons.model.MessageSpec;
import io.thingshub.commons.model.MessageType;
import io.thingshub.commons.model.Page;
import io.thingshub.dashboard.event.MessageAuthorizationEvent;
import io.thingshub.dashboard.event.ServiceClientProductBindingEvent;
import io.thingshub.dashboard.params.ServiceClientRequestParams.BindProductParams;
import io.thingshub.dashboard.params.ServiceClientRequestParams.MessageAuthorityParams;
import io.thingshub.dashboard.params.ServiceClientRequestParams.QueryServiceClientParams;
import io.thingshub.dashboard.params.ServiceClientRequestParams.ServiceClientFormParams;
import io.thingshub.dashboard.server.UserContextHolder;
import io.thingshub.dashboard.server.UserInfo;
import io.thingshub.entity.MessageAuthority;
import io.thingshub.entity.MessageDefinition;
import io.thingshub.entity.ServiceClient;
import io.thingshub.event.ApplicationEventManager;
import io.thingshub.event.ApplicationEventType;
import io.thingshub.http.HttpMethod;
import io.thingshub.http.annotation.Controller;
import io.thingshub.http.annotation.RequestBody;
import io.thingshub.http.annotation.RequestMapping;
import io.thingshub.http.annotation.RequestParam;
import io.thingshub.http.annotation.Validated;
import io.thingshub.service.MessageAuthorityService;
import io.thingshub.service.MessageDefinitionService;
import io.thingshub.service.ServiceClientService;
import io.thingshub.service.ThingModelService;
import io.thingshub.service.base.BaseService.AvailableStatus;
import io.thingshub.service.base.BaseService.DeletedStatus;
import jakarta.inject.Inject;

/**
 * <p>
 * Service Client Management API
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Controller
public class ServiceClientController {

	@Inject
	private ServiceClientService serviceClientService;

	@Inject
	private MessageAuthorityService messageAuthorityService;

	@Inject
	private ThingModelService thingModelService;

	@Inject
	private MessageDefinitionService messageDefinitionService;

	@Inject
	private ApplicationEventManager eventManager;

	@RequestMapping(path = "/service-client/query")
	public Page<ServiceClient> queryServiceClients(@Validated QueryServiceClientParams params) {
		Map<String, Object> queryParams = new HashMap<>();

		return serviceClientService.queryServiceClients(queryParams, params.getPage(), params.getSize());
	}

	@RequestMapping(path = "/service-client/info")
	public ServiceClient getServiceClient(@RequestParam Long id) {
		return serviceClientService.getServiceClient(id);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/service-client/save")
	public LongId saveServiceClient(@Validated @RequestBody ServiceClientFormParams params) throws ServiceException {
		UserInfo operator = UserContextHolder.getCurrentUser();

		Long serviceClientId = params.getId();
		if (serviceClientId == null) {
			ServiceClient serviceClient = new ServiceClient();
			BeanUtil.copyProperties(params, serviceClient, CopyOptions.create().setIgnoreNullValue(true));

			serviceClient.setStatus(AvailableStatus.NORMAL.value());
			serviceClient.setDeletedStatus(DeletedStatus.NOT_DELETED.value());
			serviceClient.setCreateBy(operator.getName());
			serviceClient.setCreateTime(DateUtil.date());
			serviceClientId = serviceClientService.createServiceClient(serviceClient);
		} else {
			ServiceClient serviceClient = serviceClientService.getByKey(params.getId());
			if (serviceClient == null) {
				throw new ServiceException("无效的客户端ID");
			}

			BeanUtil.copyProperties(params, serviceClient, CopyOptions.create().setIgnoreNullValue(true));
			serviceClientService.updateServiceClient(serviceClient);
		}

		return new LongId(serviceClientId);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/service-client/disable")
	public void disableServiceClient(@Validated @RequestBody IdParam idParam) {
		serviceClientService.updateStatus(idParam.getId(), AvailableStatus.DISABLED);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/service-client/enable")
	public void enableServiceClient(@Validated @RequestBody IdParam idParam) {
		serviceClientService.updateStatus(idParam.getId(), AvailableStatus.NORMAL);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/service-client/remove")
	public void removeServiceClient(@Validated @RequestBody IdParam idParam) {
		serviceClientService.remove(idParam.getId());
	}

	@RequestMapping(method = HttpMethod.POST, path = "/service-client/bind/product")
	public void bindProducts(@Validated @RequestBody BindProductParams params) {
		ServiceClient serviceClient = serviceClientService.bindProducts(params.getId(), params.getProductCodes());

		eventManager.publishEvent(new ServiceClientProductBindingEvent(this, serviceClient.getUsername(), params.getProductCodes(), ApplicationEventType.UPDATED.name()));
	}

	@RequestMapping(path = "/service-client/messages")
	public List<MessageAuthority> getAuthorizedMessages(@RequestParam String username, @RequestParam String productCode) {
		return messageAuthorityService.getMessageAuthorities(username, productCode);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/service-client/message/authorize")
	public void saveMessageAuthority(@Validated @RequestBody MessageAuthorityParams params) {
		List<MessageAuthority> currentMessageAuthorities = messageAuthorityService.getMessageAuthorities(params.getUsername(), params.getProductCode());
		messageAuthorityService.saveMessageAuthority(params.getUsername(), params.getProductCode(), params.getMessages());

		Set<String> currentMessageSet = Sets.newHashSet(currentMessageAuthorities.stream().map(ma -> ma.getMessageName()).toList());
		Set<String> latestMessageSet = Sets.newHashSet(params.getMessages());

		Sets.SetView<String> removedMessages = Sets.difference(currentMessageSet, latestMessageSet);
		removedMessages.forEach(m -> {
			MessageAuthorizationEvent event = new MessageAuthorizationEvent(this, params.getUsername(), params.getProductCode(), m, null, ApplicationEventType.REMOVED.name());
			eventManager.publishEvent(event);
		});

		Sets.SetView<String> addedMessages = Sets.difference(latestMessageSet, currentMessageSet);
		addedMessages.forEach(m -> {
			MessageDefinition messageDefinition = messageDefinitionService.getMessageDefinition(params.getProductCode(), m);

			MessageSpec messageSpec = new MessageSpec();
			messageSpec.setId(messageDefinition.getId());
			messageSpec.setName(messageDefinition.getName());
			messageSpec.setTitle(messageDefinition.getTitle());
			messageSpec.setProductCode(messageDefinition.getProductCode());
			messageSpec.setType(messageDefinition.getType());
			messageSpec.setType(messageDefinition.getType());
			messageSpec.setTopic(messageDefinition.getTopic());
			messageSpec.setDescription(messageDefinition.getDescription());

			JSONArray parametersInThingModel = switch (MessageType.of(messageDefinition.getType())) {
			case PROPERTY_POST -> thingModelService.getThingProperties(messageDefinition.getProductCode());
			case PROPERTY_REPLY -> null;
			case SERVICE_FUNCTION_CALL -> {
				JSONObject theService = thingModelService.getThingService(messageDefinition.getProductCode(), messageDefinition.getModelIdentifier());

				yield theService != null ? theService.getJSONArray("inputParameters") : null;
			}
			case SERVICE_FUNCTION_REPLY -> {
				JSONObject theService = thingModelService.getThingService(messageDefinition.getProductCode(), messageDefinition.getModelIdentifier());

				yield theService != null ? theService.getJSONArray("outputParameters") : null;
			}
			case SERVICE_REQUEST_POST -> {
				JSONObject theService = thingModelService.getThingService(messageDefinition.getProductCode(), messageDefinition.getModelIdentifier());

				yield theService != null ? theService.getJSONArray("inputParameters") : null;
			}
			case SERVICE_REQUEST_REPLY -> {
				JSONObject theService = thingModelService.getThingService(messageDefinition.getProductCode(), messageDefinition.getModelIdentifier());

				yield theService != null ? theService.getJSONArray("outputParameters") : null;
			}
			case EVENT_POST -> {
				JSONObject theEvent = thingModelService.getThingEvent(messageDefinition.getProductCode(), messageDefinition.getModelIdentifier());

				yield theEvent != null ? theEvent.getJSONArray("parameters") : null;
			}
			case EVENT_REPLY -> null;
			};

			List<MessageParameter> messageParameters = Collections.emptyList();
			if (parametersInThingModel != null) {
				messageParameters = parametersInThingModel.stream().map(p -> {
					JSONObject parameterObj = (JSONObject) p;

					MessageParameter messageParameter = new MessageParameter();
					messageParameter.setIdentifier(parameterObj.getString("identifier"));
					messageParameter.setName(parameterObj.getString("name"));
					messageParameter.setDataTypeSpecs(parameterObj.getObject("dataType", DataTypeSpecs.class));
					messageParameter.setRequired(parameterObj.getBooleanValue("required"));
					messageParameter.setDesc(parameterObj.getString("desc"));

					return messageParameter;
				}).toList();
			}
			messageSpec.setParameters(messageParameters);

			MessageAuthorizationEvent event = new MessageAuthorizationEvent(this, params.getUsername(), params.getProductCode(), m, messageSpec,
					ApplicationEventType.CREATED.name());
			eventManager.publishEvent(event);
		});
	}

}