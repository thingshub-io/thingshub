package io.thingshub.transport;

import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_MESSAGE_DEVICE_INFO_ACK;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_MESSAGE_DISABLE_DEVICE_ACK;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_MESSAGE_ENABLE_DEVICE_ACK;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_MESSAGE_QUERY_DEVICE_ACK;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_MESSAGE_QUERY_MESSAGE_MODEL_ACK;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_TOPIC_BOUND_PRODUCT_QUERY;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_TOPIC_BOUND_PRODUCT_QUERY_REPLY;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_TOPIC_DEVICE_DISABLE;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_TOPIC_DEVICE_DISABLE_REPLY;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_TOPIC_DEVICE_ENABLE;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_TOPIC_DEVICE_ENABLE_REPLY;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_TOPIC_DEVICE_INFO;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_TOPIC_DEVICE_INFO_REPLY;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_TOPIC_DEVICE_QUERY;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_TOPIC_DEVICE_QUERY_REPLY;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_TOPIC_MESSAGE_MODEL_QUERY;
import static io.thingshub.commons.model.ThingshubConstants.INTERNAL_TOPIC_MESSAGE_MODEL_QUERY_REPLY;
import static io.thingshub.subscribe.TopicUtils.isInternalTopic;
import static io.thingshub.subscribe.TopicUtils.isTopicMatch;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.Maps;

import cn.hutool.core.date.DateUtil;
import io.thingshub.Broker;
import io.thingshub.commons.model.MessageModel;
import io.thingshub.commons.model.MessageResult;
import io.thingshub.commons.model.Page;
import io.thingshub.commons.model.ThingshubMessage;
import io.thingshub.config.TenantSettings;
import io.thingshub.entity.ClientUser;
import io.thingshub.entity.Device;
import io.thingshub.entity.MessageSpec;
import io.thingshub.entity.Publication;
import io.thingshub.entity.Session;
import io.thingshub.ioc.Component;
import io.thingshub.service.ClientUserService;
import io.thingshub.service.DeviceService;
import io.thingshub.service.InboxService;
import io.thingshub.service.MessageSpecService;
import io.thingshub.service.PublicationService;
import io.thingshub.service.ThingModelService;
import io.thingshub.service.base.IdGenerator;
import io.thingshub.service.model.Delivery;
import io.thingshub.subscribe.MatchedSubscriber;
import io.thingshub.subscribe.SubscriptionManager;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Distribute published message to matched subscribers
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Component
@Slf4j
public class PublicationDistributor {

	@Inject
	private PublicationService publicationService;

	@Inject
	private ClientUserService clientUserService;

	@Inject
	private ThingModelService thingModelService;

	@Inject
	private MessageSpecService messageSpecService;

	@Inject
	private DeviceService deviceService;

	@Inject
	private SubscriptionManager subscriptionManager;

	@Inject
	private InboxService inboxService;

	@Inject
	private ConnectionManager connectionManager;

	@Inject
	private SessionManager sessionManager;

	@Inject
	private IdGenerator idGenerator;

	@PostConstruct
	public void init() {
		publicationService.listen((eventType, publication) -> {
			switch (eventType) {
			case CREATED -> {
				/**
				 * process internal message sent by thingshub client
				 */
				if (isInternalTopic(publication.getStdTopic())) {
					handleInternalMessage(publication);
				}

				/**
				 * process normal message sent by device or thingshub client
				 */
				else {
					Set<MatchedSubscriber> matchedSubscribers = subscriptionManager.match(publication.getStdTopic());
					matchedSubscribers.stream().filter(s -> {
						return !s.getSubscriberId().equals(publication.getPublisherId())
								|| (s.getSubscriberId().equals(publication.getPublisherId()) && Boolean.FALSE.equals(s.getProps().get("noLocal")));
					}).collect(Collectors.groupingBy(s -> Optional.ofNullable(s.getGroup()).orElse(s.getSubscriberId()))).forEach((grp, subs) -> {
						Integer qos = (Integer) publication.getProps().get("qos");
						MatchedSubscriber selectedSubscriber = null;

						List<MatchedSubscriber> onlineSubscriptions = subs.stream().filter(match -> connectionManager.isOnline(match.getSubscriberId())).toList();
						if (onlineSubscriptions.size() > 0) {
							int index = ThreadLocalRandom.current().nextInt(onlineSubscriptions.size());
							selectedSubscriber = onlineSubscriptions.get(index);
						} else {
							if (qos != null && qos.intValue() > 0) {
								int index = ThreadLocalRandom.current().nextInt(subs.size());
								selectedSubscriber = subs.get(index);
							}
						}

						if (selectedSubscriber != null) {
							Session sessionOfSuscriber = sessionManager.getClientSession(selectedSubscriber.getSubscriberId());
							if (sessionOfSuscriber == null) {// 极端情况导致的订阅脏数据
								return;
							}

							log.debug("Selected subscriber. subscriber id: {}", selectedSubscriber.getSubscriberId());

							if (subscriptionManager.confirmMessageReceiver(publication.getId(), grp, selectedSubscriber.getSubscriberId())) {
								long deliveryId = idGenerator.nextId();

								Delivery delivery = Delivery.builder().id(deliveryId).receiverId(selectedSubscriber.getSubscriberId()).recProps(selectedSubscriber.getProps())
										.senderId(publication.getPublisherId()).sendProps(publication.getProps()).topic(publication.getTopic()).payload(publication.getStdPayload())
										.deliverySource(DeliverySource.of(publication.getPubMethod())).deliverTime(new Date()).build();

								// session is persistent and client is offline
								if (sessionOfSuscriber.getExpireTime() != null) {
									long deliveryExpiryInterval = Duration.ofMillis(sessionOfSuscriber.getExpireTime().getTime() - System.currentTimeMillis()).toSeconds();
									inboxService.deliver(delivery, deliveryExpiryInterval, TimeUnit.SECONDS);
								}
								// session is persistent and client is online
								else if (sessionOfSuscriber.getExpiryInterval() != null && sessionOfSuscriber.getExpiryInterval().intValue() > 0) {
									inboxService.deliver(delivery, sessionOfSuscriber.getExpiryInterval().longValue(), TimeUnit.SECONDS);
								}
								// session is transient and client is online
								else {
									inboxService.deliver(delivery, TenantSettings.DFT_MAX_SESSION_EXPIRY_INTERVAL, TimeUnit.SECONDS);
								}
							}
						}
					});
				}
			}
			default -> {
			}
			}
		});
	}

	private void handleInternalMessage(Publication publication) {
		if (isTopicMatch(INTERNAL_TOPIC_BOUND_PRODUCT_QUERY, publication.getStdTopic())) {
			handleBoundProductQuery(publication);
		} else if (isTopicMatch(INTERNAL_TOPIC_MESSAGE_MODEL_QUERY, publication.getStdTopic())) {
			handleMessageModelQuery(publication);
		} else if (isTopicMatch(INTERNAL_TOPIC_DEVICE_QUERY, publication.getStdTopic())) {
			handleDeviceQuery(publication);
		} else if (isTopicMatch(INTERNAL_TOPIC_DEVICE_INFO, publication.getStdTopic())) {
			handleDeviceInfo(publication);
		} else if (isTopicMatch(INTERNAL_TOPIC_DEVICE_DISABLE, publication.getStdTopic())) {
			handleDeviceDisable(publication);
		} else if (isTopicMatch(INTERNAL_TOPIC_DEVICE_ENABLE, publication.getStdTopic())) {
			handleDeviceEnable(publication);
		}
	}

	private void createPublication4Reply(String topic, ThingshubMessage payload, Date expiredAt) {
		String strPayload = JSON.toJSONString(payload);

		Map<String, Object> props = Maps.newHashMap();
		props.put("qos", 2);

		long messageExpiryInterval = Duration.ofMillis(expiredAt.getTime() - System.currentTimeMillis()).toSeconds();

		long publicationId = idGenerator.nextId();
		Publication publication = new Publication();
		publication.setId(publicationId);
		publication.setPublisherId("sys");
		publication.setPubMethod(PubMethod.PUBLISH.value());
		publication.setTopic(topic);
		publication.setStdTopic(topic);
		publication.setProps(props);
		publication.setPayload(strPayload);
		publication.setStdPayload(strPayload);
		publication.setExpireTime(expiredAt);
		publication.setTimestamp(System.currentTimeMillis());
		publicationService.save(publicationId, publication, messageExpiryInterval, TimeUnit.SECONDS);
	}

	private void handleBoundProductQuery(Publication publication) {
		JSONObject stdMessage = JSON.parseObject(publication.getPayload());
		String clientId = stdMessage.getString("clientId");
		JSONObject params = stdMessage.getJSONObject("params");
		String username = params.getString("username");
		String topic4Reply = String.format(INTERNAL_TOPIC_BOUND_PRODUCT_QUERY_REPLY, username, clientId);

		List<String> boundProducts = Collections.emptyList();
		ClientUser clientUser = clientUserService.getClientUser(username);
		if (clientUser != null && clientUser.getProductCodes() != null) {
			boundProducts = JSON.parseArray(clientUser.getProductCodes()).toJavaList(String.class);
		}

		ThingshubMessage replyMessage = new ThingshubMessage();
		replyMessage.setId(UUID.randomUUID().toString());
		replyMessage.setClientId(Broker.getConsistentId());
		replyMessage.setVersion("1.0.0");
		replyMessage.setName(INTERNAL_MESSAGE_QUERY_MESSAGE_MODEL_ACK);
		replyMessage.setTime(DateUtil.now());
		replyMessage.setCode(MessageResult.SUCCESS.code());
		replyMessage.setMessage(MessageResult.SUCCESS.desc());
		replyMessage.setData(boundProducts);

		createPublication4Reply(topic4Reply, replyMessage, publication.getExpireTime());
	}

	private void handleMessageModelQuery(Publication publication) {
		JSONObject stdMessage = JSON.parseObject(publication.getPayload());
		String clientId = stdMessage.getString("clientId");
		JSONObject params = stdMessage.getJSONObject("params");
		String username = params.getString("username");
		String productCode = params.getString("productCode");
		String topic4Reply = String.format(INTERNAL_TOPIC_MESSAGE_MODEL_QUERY_REPLY, username, clientId);

		List<MessageModel> messageModels = Collections.emptyList();
		List<MessageSpec> messageSpecs = messageSpecService.getMessageSpecs(username, productCode);
		if (messageSpecs != null) {
			Map<String, JSONObject> parametersInThingModel = thingModelService.getParametersInThingModel(productCode);

			messageModels = messageSpecs.stream().map(msgSpec -> {
				return messageSpecService.messageSpec2MessageModel(msgSpec, parametersInThingModel);
			}).toList();
		}

		ThingshubMessage replyMessage = new ThingshubMessage();
		replyMessage.setId(UUID.randomUUID().toString());
		replyMessage.setClientId(Broker.getConsistentId());
		replyMessage.setVersion("1.0.0");
		replyMessage.setName(INTERNAL_MESSAGE_QUERY_MESSAGE_MODEL_ACK);
		replyMessage.setTime(DateUtil.now());
		replyMessage.setCode(MessageResult.SUCCESS.code());
		replyMessage.setMessage(MessageResult.SUCCESS.desc());
		replyMessage.setData(messageModels);

		createPublication4Reply(topic4Reply, replyMessage, publication.getExpireTime());
	}

	private void handleDeviceQuery(Publication publication) {
		JSONObject stdMessage = JSON.parseObject(publication.getPayload());
		String clientId = stdMessage.getString("clientId");
		JSONObject params = stdMessage.getJSONObject("params");
		String username = params.getString("username");

		JSONObject queryCriterions = params.getJSONObject("queryCriterions");
		String topic4Reply = String.format(INTERNAL_TOPIC_DEVICE_QUERY_REPLY, username, clientId);

		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("tenantId", queryCriterions.getString("tenantId"));
		queryParams.put("customerId", queryCriterions.getString("customerId"));
		queryParams.put("region", queryCriterions.getString("region"));
		queryParams.put("group", queryCriterions.getString("group"));
		queryParams.put("productCode", queryCriterions.getString("productCode"));
		queryParams.put("sn", queryCriterions.getString("sn"));
		Page<Device> devices = deviceService.queryDevices(queryParams, queryCriterions.getIntValue("page"), queryCriterions.getIntValue("size"));

		ThingshubMessage replyMessage = new ThingshubMessage();
		replyMessage.setId(UUID.randomUUID().toString());
		replyMessage.setClientId(Broker.getConsistentId());
		replyMessage.setVersion("1.0.0");
		replyMessage.setName(INTERNAL_MESSAGE_QUERY_DEVICE_ACK);
		replyMessage.setTime(DateUtil.now());
		replyMessage.setCode(MessageResult.SUCCESS.code());
		replyMessage.setMessage(MessageResult.SUCCESS.desc());
		replyMessage.setData(devices);

		createPublication4Reply(topic4Reply, replyMessage, publication.getExpireTime());
	}

	private void handleDeviceInfo(Publication publication) {
		JSONObject stdMessage = JSON.parseObject(publication.getPayload());
		String clientId = stdMessage.getString("clientId");
		JSONObject params = stdMessage.getJSONObject("params");
		String username = params.getString("username");
		String sn = params.getString("sn");
		String topic4Reply = String.format(INTERNAL_TOPIC_DEVICE_INFO_REPLY, username, clientId);

		Device device = deviceService.getBySn(sn);

		ThingshubMessage replyMessage = new ThingshubMessage();
		replyMessage.setId(UUID.randomUUID().toString());
		replyMessage.setClientId(Broker.getConsistentId());
		replyMessage.setVersion("1.0.0");
		replyMessage.setName(INTERNAL_MESSAGE_DEVICE_INFO_ACK);
		replyMessage.setTime(DateUtil.now());
		replyMessage.setCode(MessageResult.SUCCESS.code());
		replyMessage.setMessage(MessageResult.SUCCESS.desc());
		replyMessage.setData(device);

		createPublication4Reply(topic4Reply, replyMessage, publication.getExpireTime());
	}

	private void handleDeviceDisable(Publication publication) {
		JSONObject stdMessage = JSON.parseObject(publication.getPayload());
		JSONObject params = stdMessage.getJSONObject("params");
		String clientId = stdMessage.getString("clientId");

		String username = params.getString("username");
		String productCode = params.getString("productCode");
		String sn = params.getString("sn");
		String topic4Reply = String.format(INTERNAL_TOPIC_DEVICE_DISABLE_REPLY, username, clientId);

		// TODO 检查操作者权限
		deviceService.disable(productCode, sn);

		ThingshubMessage replyMessage = new ThingshubMessage();
		replyMessage.setId(UUID.randomUUID().toString());
		replyMessage.setClientId(Broker.getConsistentId());
		replyMessage.setVersion("1.0.0");
		replyMessage.setName(INTERNAL_MESSAGE_DISABLE_DEVICE_ACK);
		replyMessage.setTime(DateUtil.now());
		replyMessage.setCode(MessageResult.SUCCESS.code());
		replyMessage.setMessage(MessageResult.SUCCESS.desc());
		replyMessage.setData(null);

		createPublication4Reply(topic4Reply, replyMessage, publication.getExpireTime());
	}

	private void handleDeviceEnable(Publication publication) {
		JSONObject stdMessage = JSON.parseObject(publication.getPayload());
		JSONObject params = stdMessage.getJSONObject("params");
		String clientId = stdMessage.getString("clientId");

		String username = params.getString("username");
		String productCode = params.getString("productCode");
		String sn = params.getString("sn");
		String topic4Reply = String.format(INTERNAL_TOPIC_DEVICE_ENABLE_REPLY, username, clientId);

		// TODO 检查操作者权限
		deviceService.enable(productCode, sn);

		ThingshubMessage replyMessage = new ThingshubMessage();
		replyMessage.setId(UUID.randomUUID().toString());
		replyMessage.setClientId(Broker.getConsistentId());
		replyMessage.setVersion("1.0.0");
		replyMessage.setName(INTERNAL_MESSAGE_ENABLE_DEVICE_ACK);
		replyMessage.setTime(DateUtil.now());
		replyMessage.setCode(MessageResult.SUCCESS.code());
		replyMessage.setMessage(MessageResult.SUCCESS.desc());
		replyMessage.setData(null);

		createPublication4Reply(topic4Reply, replyMessage, publication.getExpireTime());

	}

}
