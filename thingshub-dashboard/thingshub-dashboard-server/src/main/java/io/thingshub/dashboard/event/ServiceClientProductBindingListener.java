package io.thingshub.dashboard.event;

import static io.thingshub.commons.model.ThingshubConstants.SERVICE_CLIENT_ALL;
import static io.thingshub.commons.model.ThingshubConstants.SERVICE_CLIENT_INTERNAL_MESSAGE_CHANGE_PRODUCT_BINDING;
import static io.thingshub.commons.model.ThingshubConstants.SERVICE_CLIENT_INTERNAL_TOPIC_CHANGE_PRODUCT_BINDING;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson2.JSONObject;

import io.thingshub.Broker;
import io.thingshub.entity.Publication;
import io.thingshub.event.ApplicationListener;
import io.thingshub.ioc.Component;
import io.thingshub.service.PublicationService;
import io.thingshub.service.base.IdGenerator;
import io.thingshub.transport.PublishWay;
import jakarta.inject.Inject;

/**
 * <p>
 * Listen service client's product binding event
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Component
public class ServiceClientProductBindingListener implements ApplicationListener<ServiceClientProductBindingEvent> {

	@Inject
	private IdGenerator idGenerator;

	@Inject
	private PublicationService publicationService;

	@Override
	public void onApplicationEvent(ServiceClientProductBindingEvent event) {
		Map<String, Object> props = new HashMap<>();
		props.put("qos", Integer.valueOf(2));

		JSONObject stdMessage = new JSONObject();
		stdMessage.put("id", UUID.randomUUID().toString());
		stdMessage.put("clientId", Broker.getConsistentId());
		stdMessage.put("version", "1.0.0");
		stdMessage.put("name", SERVICE_CLIENT_INTERNAL_MESSAGE_CHANGE_PRODUCT_BINDING);
		stdMessage.put("timestamp", System.currentTimeMillis());

		JSONObject paramsObj = new JSONObject();
		paramsObj.put("productCodes", event.getProductCodes());
		paramsObj.put("action", event.getAction());

		stdMessage.put("params", paramsObj);

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 120);

		long publicationId = idGenerator.nextId();
		Publication publication = new Publication();
		publication.setId(publicationId);
		publication.setUsername("sys");
		publication.setClientId(Broker.getConsistentId());
		publication.setPubWay(PublishWay.PUBLISH.value());
		publication.setTopic(String.format(SERVICE_CLIENT_INTERNAL_TOPIC_CHANGE_PRODUCT_BINDING, event.getUsername(), SERVICE_CLIENT_ALL));
		publication.setStdTopic(String.format(SERVICE_CLIENT_INTERNAL_TOPIC_CHANGE_PRODUCT_BINDING, event.getUsername(), SERVICE_CLIENT_ALL));
		publication.setProps(props);
		publication.setPayload(stdMessage.toJSONString());
		publication.setStdPayload(stdMessage.toJSONString());
		publication.setExpireTime(calendar.getTime());
		publication.setTimestamp(System.currentTimeMillis());
		publicationService.save(publicationId, publication, 120, TimeUnit.SECONDS);
	}

}
