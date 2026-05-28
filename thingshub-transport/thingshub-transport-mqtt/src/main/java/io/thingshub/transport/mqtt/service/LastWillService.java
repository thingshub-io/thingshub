package io.thingshub.transport.mqtt.service;

import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.CONTENT_TYPE;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.CORRELATION_DATA;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.PAYLOAD_FORMAT_INDICATOR;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.RESPONSE_TOPIC;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.USER_PROPERTY;
import static io.thingshub.commons.model.ThingshubConstants.THING_TOPIC_EVENT_POST;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;

import cn.hutool.db.sql.Condition;
import io.netty.handler.codec.mqtt.MqttProperties.UserProperty;
import io.thingshub.Broker;
import io.thingshub.entity.GroupSelection;
import io.thingshub.entity.Publication;
import io.thingshub.entity.ScriptInfo;
import io.thingshub.ioc.Service;
import io.thingshub.script.ScriptEngine;
import io.thingshub.script.ScriptEngineFactory;
import io.thingshub.service.GroupSelectionService;
import io.thingshub.service.GroupSelectionService.GroupSelectionKey;
import io.thingshub.service.PublicationService;
import io.thingshub.service.ScriptService;
import io.thingshub.service.base.BaseService;
import io.thingshub.service.base.IdGenerator;
import io.thingshub.service.model.ClientType;
import io.thingshub.transport.ConnectionManager;
import io.thingshub.transport.PublishWay;
import io.thingshub.transport.SessionManager;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Process Last Will Message's Publishing
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
@Slf4j
public class LastWillService extends BaseService<Long, LastWill> {

	@Inject
	private ScriptService scriptService;

	@Inject
	private ScriptEngineFactory scriptEngineFactory;

	@Inject
	private PublicationService publicationService;

	@Inject
	private RetainService retainService;

	@Inject
	private ConnectionManager connStateManager;

	@Inject
	private SessionManager sessionManager;

	@Inject
	private IdGenerator idGenerator;

	@Inject
	private GroupSelectionService groupSelectionService;

	@PostConstruct
	public void init() {
		this.listen((eventType, lw) -> {
			switch (eventType) {
			case EXPIRED -> {
				if (confirmPubNode(lw.getId(), "LAST_WILL_DELAY", Broker.getConsistentId())) {
					// MQTT-3.1.3-9
					boolean pubLastWill = false;
					if (!connStateManager.isOnline(lw.getClientId())) { // Not reconnect yet before last will expired
						if (log.isDebugEnabled()) {
							log.debug("Client [{}] is offline", lw.getClientId());
						}
						pubLastWill = true;
					} else {
						if (!sessionManager.isSessionExists(lw.getSessionId())) {// reconnect with clean start = 1
							if (log.isDebugEnabled()) {
								log.debug("Session [{}] is cleaned ", lw.getSessionId());
							}
							pubLastWill = true;
						}
					}

					if (pubLastWill) {
						Map<String, Object> props = new HashMap<>();
						props.put("qos", lw.getQos());

						if (lw.getPayloadFormatIndicator() != null) {
							props.put(String.valueOf(PAYLOAD_FORMAT_INDICATOR.value()), lw.getPayloadFormatIndicator().booleanValue() ? 1 : 0);
						}
						if (lw.getContentType() != null) {
							props.put(String.valueOf(CONTENT_TYPE.value()), lw.getContentType());
						}
						if (lw.getResponseTopic() != null) {
							props.put(String.valueOf(RESPONSE_TOPIC.value()), lw.getResponseTopic());
						}
						if (lw.getCorrelationData() != null) {
							props.put(String.valueOf(CORRELATION_DATA.value()), lw.getCorrelationData().getBytes(StandardCharsets.UTF_8));
						}
						if (lw.getUserProperties() != null) {
							List<UserProperty> ups = Lists.newArrayList();
							lw.getUserProperties().forEach(sp -> ups.add(new UserProperty(sp.key, sp.value)));
							props.put(String.valueOf(USER_PROPERTY.value()), ups);
						}

						String stdTopic = lw.getTopic();
						String payload = lw.getPayload();
						if (ClientType.DEVICE_CLIENT == ClientType.of(lw.getClientType())) {
							stdTopic = String.format(THING_TOPIC_EVENT_POST, lw.getProductCode(), lw.getClientId(), "lastwill");

							ScriptInfo transformScript = scriptService.getScript(lw.getProductCode());
							if (transformScript != null) {
								ScriptEngine scriptEngine = scriptEngineFactory.getScriptEngine(transformScript.getLang());
								payload = scriptEngine.invoke(transformScript.getName(), String.class, "lastwill", lw.getClientId(),
										lw.getPayload().getBytes(StandardCharsets.UTF_8));
							}
						}

						Calendar calendar = Calendar.getInstance();
						calendar.add(Calendar.SECOND, lw.getExpiryInterval());

						long publicationId = idGenerator.nextId();
						Publication publication = new Publication();
						publication.setId(publicationId);
						publication.setUsername(lw.getUsername());
						publication.setClientId(lw.getClientId());
						publication.setPubWay(PublishWay.LASTWILL.value());
						publication.setTopic(lw.getTopic());
						publication.setStdTopic(stdTopic);
						publication.setProps(props);
						publication.setPayload(lw.getPayload());
						publication.setStdPayload(payload);
						publication.setExpireTime(calendar.getTime());
						publication.setTimestamp(System.currentTimeMillis());
						publicationService.save(publicationId, publication, lw.getExpiryInterval(), TimeUnit.SECONDS);

						if (lw.getRetain()) {
							if (lw.getPayload() == null || lw.getPayload().length() == 0) {
								retainService.cleanRetain(lw.getTopic());
							} else {
								long retainId = idGenerator.nextId();

								Retain retain = new Retain();
								retain.setId(retainId);
								retain.setTopic(lw.getTopic());
								retain.setStdTopic(stdTopic);
								retain.setPayload(payload);
								retain.setProps(publication.getProps());
								retain.setPublisherId(lw.getClientId());
								retain.setCreateTime(new Date());

								retainService.saveRetain(retain, lw.getExpiryInterval(), TimeUnit.SECONDS);
							}
						}
					}
				}
			}
			default -> {
			}
			}
		});
	}

	private boolean confirmPubNode(Long lastWillId, String group, String nodeId) {
		GroupSelection groupSelection = new GroupSelection();
		groupSelection.setDataId(lastWillId.toString());
		groupSelection.setGroup(group);
		groupSelection.setWinner(nodeId);
		groupSelection.setCreateTime(new Date());
		GroupSelectionKey theKey = new GroupSelectionKey(lastWillId.toString(), group);

		return groupSelectionService.saveIfAbsent(theKey, groupSelection, 10 * 60, SECONDS);
	}

	public void cleanLassWillOfSession(Long sessionId) {
		this.remove(Lists.newArrayList(new Condition("session_id", sessionId)));
	}

}