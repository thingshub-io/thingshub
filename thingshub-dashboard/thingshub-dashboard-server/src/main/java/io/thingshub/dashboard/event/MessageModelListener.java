package io.thingshub.dashboard.event;

import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_MESSAGE_MESSAGE_MODEL_CHANGED;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_TOPIC_MESSAGE_MODEL_CHANGED;

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
 * message specification change listening
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Component
public class MessageModelListener implements ApplicationListener<MessageModelEvent> {

	@Inject
	private IdGenerator idGenerator;

	@Inject
	private PublicationService publicationService;

	@Override
	public void onApplicationEvent(MessageModelEvent event) {
		Map<String, Object> props = new HashMap<>();
		props.put("qos", Integer.valueOf(2));

		JSONObject stdMessage = new JSONObject();
		stdMessage.put("id", UUID.randomUUID().toString());
		stdMessage.put("clientId", "sys");
		stdMessage.put("version", "1.0.0");
		stdMessage.put("name", INTERNAL_MESSAGE_MESSAGE_MODEL_CHANGED);
		stdMessage.put("timestamp", System.currentTimeMillis());

		JSONObject paramsObj = new JSONObject();
		paramsObj.put("productCode", event.getProductCode());
		paramsObj.put("name", event.getName());
		paramsObj.put("parameters", event.getParameters());
		paramsObj.put("action", event.getAction());

		stdMessage.put("params", paramsObj);

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 120);

		long publicationId = idGenerator.nextId();
		Publication publication = new Publication();
		publication.setId(publicationId);
		publication.setPublisherId("sys");
		publication.setPubMethod(PubMethod.PUBLISH.value());
		publication.setTopic(String.format(INTERNAL_TOPIC_MESSAGE_MODEL_CHANGED, event.getProductCode()));
		publication.setStdTopic(String.format(INTERNAL_TOPIC_MESSAGE_MODEL_CHANGED, event.getProductCode()));
		publication.setProps(props);
		publication.setPayload(stdMessage.toJSONString());
		publication.setStdPayload(stdMessage.toJSONString());
		publication.setExpireTime(calendar.getTime());
		publication.setTimestamp(System.currentTimeMillis());
		publicationService.save(publicationId, publication, 120, TimeUnit.SECONDS);
	}

}
