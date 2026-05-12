package io.thingshub.dashboard.event;

import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_MESSAGE_BOUND_PRODUCT_CHANGED;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_TOPIC_BOUND_PRODUCT_CHANGED;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson2.JSONObject;

import io.thingshub.entity.Publication;
import io.thingshub.event.ApplicationListener;
import io.thingshub.ioc.Component;
import io.thingshub.service.PublicationService;
import io.thingshub.service.base.IdGenerator;
import io.thingshub.transport.PubMethod;
import jakarta.inject.Inject;

/**
 * <p>
 * client user bind product listening
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Component
public class ClientUserProductListener implements ApplicationListener<ClienntUserProductEvent> {

	@Inject
	private IdGenerator idGenerator;

	@Inject
	private PublicationService publicationService;

	@Override
	public void onApplicationEvent(ClienntUserProductEvent event) {
		Map<String, Object> props = new HashMap<>();
		props.put("qos", Integer.valueOf(2));

		JSONObject stdMessage = new JSONObject();
		stdMessage.put("id", UUID.randomUUID().toString());
		stdMessage.put("clientId", "sys");
		stdMessage.put("version", "1.0.0");
		stdMessage.put("name", INTERNAL_MESSAGE_BOUND_PRODUCT_CHANGED);
		stdMessage.put("timestamp", System.currentTimeMillis());

		JSONObject paramsObj = new JSONObject();
		paramsObj.put("username", event.getUsername());
		paramsObj.put("productCodes", event.getProductCodes());
		paramsObj.put("action", event.getAction());

		stdMessage.put("params", paramsObj);

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 120);

		long publicationId = idGenerator.nextId();
		Publication publication = new Publication();
		publication.setId(publicationId);
		publication.setPublisherId("sys");
		publication.setPubMethod(PubMethod.PUBLISH.value());
		publication.setTopic(String.format(INTERNAL_TOPIC_BOUND_PRODUCT_CHANGED, event.getUsername()));
		publication.setStdTopic(String.format(INTERNAL_TOPIC_BOUND_PRODUCT_CHANGED, event.getUsername()));
		publication.setProps(props);
		publication.setPayload(stdMessage.toJSONString());
		publication.setStdPayload(stdMessage.toJSONString());
		publication.setExpireTime(calendar.getTime());
		publication.setTimestamp(System.currentTimeMillis());
		publicationService.save(publicationId, publication, 120, TimeUnit.SECONDS);
	}

}
