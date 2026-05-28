package io.thingshub.transport;

import static io.thingshub.commons.model.ThingshubConstants.SERVICE_CLIENT_INTERNAL_MESSAGE_DEVICE_INFO;
import static io.thingshub.commons.model.ThingshubConstants.SERVICE_CLIENT_INTERNAL_MESSAGE_DEVICE_INFO_ACK;
import static io.thingshub.commons.model.ThingshubConstants.SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_DEVICE;
import static io.thingshub.commons.model.ThingshubConstants.SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_DEVICE_ACK;
import static io.thingshub.commons.model.ThingshubConstants.SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_MESSAGE_DEFINITION;
import static io.thingshub.commons.model.ThingshubConstants.SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_MESSAGE_DEFINITION_ACK;
import static io.thingshub.commons.model.ThingshubConstants.SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_PRODUCT_BINDING;
import static io.thingshub.commons.model.ThingshubConstants.SERVICE_CLIENT_INTERNAL_TOPIC_DEVICE_INFO_REPLY;
import static io.thingshub.commons.model.ThingshubConstants.SERVICE_CLIENT_INTERNAL_TOPIC_QUERY_DEVICE_REPLY;
import static io.thingshub.commons.model.ThingshubConstants.SERVICE_CLIENT_INTERNAL_TOPIC_QUERY_MESSAGE_DEFINITION_REPLY;
import static io.thingshub.commons.model.ThingshubConstants.SERVICE_CLIENT_INTERNAL_TOPIC_QUERY_PRODUCT_BINDING_REPLY;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.Maps;

import cn.hutool.core.date.DateUtil;
import io.thingshub.Broker;
import io.thingshub.commons.model.DataTypeSpecs;
import io.thingshub.commons.model.MessageParameter;
import io.thingshub.commons.model.MessageResult;
import io.thingshub.commons.model.MessageSpec;
import io.thingshub.commons.model.MessageType;
import io.thingshub.commons.model.Page;
import io.thingshub.commons.model.ThingshubMessage;
import io.thingshub.config.TenantSettings;
import io.thingshub.entity.Device;
import io.thingshub.entity.MessageDefinition;
import io.thingshub.entity.Publication;
import io.thingshub.entity.ServiceClient;
import io.thingshub.entity.Session;
import io.thingshub.ioc.Component;
import io.thingshub.service.DeviceService;
import io.thingshub.service.InboxService;
import io.thingshub.service.MessageDefinitionService;
import io.thingshub.service.PublicationService;
import io.thingshub.service.ServiceClientService;
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

	private final Map<String, Consumer<Publication>> internal_request_handlers = Maps.newHashMap();

	@Inject
	private PublicationService publicationService;

	@Inject
	private ServiceClientService serviceClientService;

	@Inject
	private ThingModelService thingModelService;

	@Inject
	private MessageDefinitionService messageDefinitionService;

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
		internal_request_handlers.put(SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_PRODUCT_BINDING, this::handleProductBingdingQuery);
		internal_request_handlers.put(SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_MESSAGE_DEFINITION, this::handleMessageDefinitionQuery);
		internal_request_handlers.put(SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_DEVICE, this::handleDeviceQuery);
		internal_request_handlers.put(SERVICE_CLIENT_INTERNAL_MESSAGE_DEVICE_INFO, this::handleDeviceInfo);

		publicationService.listen((eventType, publication) -> {
			switch (eventType) {
			case CREATED -> {
				String messageName = publication.getStdTopic().substring(publication.getStdTopic().lastIndexOf("/") + 1);
				Consumer<Publication> internalRequestHandler = internal_request_handlers.get(messageName);

				if (internalRequestHandler != null) {
					internalRequestHandler.accept(publication);
				} else {
					Set<MatchedSubscriber> matchedSubscribers = subscriptionManager.match(publication.getStdTopic());
					matchedSubscribers.stream().filter(s -> {
						return !s.getSubscriberId().equals(publication.getClientId())
								|| (s.getSubscriberId().equals(publication.getClientId()) && Boolean.FALSE.equals(s.getProps().get("noLocal")));
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
										.senderId(publication.getClientId()).sendProps(publication.getProps()).topic(publication.getTopic()).payload(publication.getStdPayload())
										.deliverySource(DeliverySource.of(publication.getPubWay())).deliverTime(new Date()).build();

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

	private void createPublication4Reply(String topic, ThingshubMessage payload, Date expiredAt) {
		String strPayload = JSON.toJSONString(payload);

		Map<String, Object> props = Maps.newHashMap();
		props.put("qos", 2);

		long messageExpiryInterval = Duration.ofMillis(expiredAt.getTime() - System.currentTimeMillis()).toSeconds();

		long publicationId = idGenerator.nextId();
		Publication publication = new Publication();
		publication.setId(publicationId);
		publication.setUsername("sys");
		publication.setClientId(Broker.getConsistentId());
		publication.setPubWay(PublishWay.PUBLISH.value());
		publication.setTopic(topic);
		publication.setStdTopic(topic);
		publication.setProps(props);
		publication.setPayload(strPayload);
		publication.setStdPayload(strPayload);
		publication.setExpireTime(expiredAt);
		publication.setTimestamp(System.currentTimeMillis());
		publicationService.save(publicationId, publication, messageExpiryInterval, TimeUnit.SECONDS);
	}

	private void handleProductBingdingQuery(Publication publication) {
		JSONObject stdMessage = JSON.parseObject(publication.getPayload());
		String messageId = stdMessage.getString("id");
		String clientId = stdMessage.getString("clientId");
		String topic4Reply = String.format(SERVICE_CLIENT_INTERNAL_TOPIC_QUERY_PRODUCT_BINDING_REPLY, publication.getUsername(), clientId);

		List<String> boundProducts = Collections.emptyList();
		ServiceClient serviceClient = serviceClientService.getServiceClient(publication.getUsername());
		if (serviceClient != null && serviceClient.getProductCodes() != null) {
			boundProducts = JSON.parseArray(serviceClient.getProductCodes()).toJavaList(String.class);
		}

		ThingshubMessage replyMessage = new ThingshubMessage();
		replyMessage.setId(messageId);
		replyMessage.setClientId(Broker.getConsistentId());
		replyMessage.setVersion("1.0.0");
		replyMessage.setName(SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_MESSAGE_DEFINITION_ACK);
		replyMessage.setTime(DateUtil.now());
		replyMessage.setCode(MessageResult.SUCCESS.code());
		replyMessage.setMessage(MessageResult.SUCCESS.desc());
		replyMessage.setData(boundProducts);

		createPublication4Reply(topic4Reply, replyMessage, publication.getExpireTime());
	}

	private void handleMessageDefinitionQuery(Publication publication) {
		JSONObject stdMessage = JSON.parseObject(publication.getPayload());
		String messageId = stdMessage.getString("id");
		String clientId = stdMessage.getString("clientId");
		JSONObject params = stdMessage.getJSONObject("params");
		String productCode = params.getString("productCode");
		String topic4Reply = String.format(SERVICE_CLIENT_INTERNAL_TOPIC_QUERY_MESSAGE_DEFINITION_REPLY, publication.getUsername(), clientId);

		List<MessageSpec> messageSpecs = Collections.emptyList();
		List<MessageDefinition> authorizedMessages = messageDefinitionService.getAuthorizedMessages(publication.getUsername(), productCode);
		if (authorizedMessages != null) {
			messageSpecs = authorizedMessages.stream().map(m -> {
				MessageSpec messageSpec = new MessageSpec();
				messageSpec.setId(m.getId());
				messageSpec.setName(m.getName());
				messageSpec.setTitle(m.getTitle());
				messageSpec.setProductCode(m.getProductCode());
				messageSpec.setType(m.getType());
				messageSpec.setTopic(m.getTopic());
				messageSpec.setDescription(m.getDescription());

				JSONArray parametersInThingModel = switch (MessageType.of(m.getType())) {
				case PROPERTY_POST -> thingModelService.getThingProperties(m.getProductCode());
				case PROPERTY_REPLY -> null;
				case SERVICE_FUNCTION_CALL -> {
					JSONObject theService = thingModelService.getThingService(m.getProductCode(), m.getModelIdentifier());

					yield theService != null ? theService.getJSONArray("inputParameters") : null;
				}
				case SERVICE_FUNCTION_REPLY -> {
					JSONObject theService = thingModelService.getThingService(m.getProductCode(), m.getModelIdentifier());

					yield theService != null ? theService.getJSONArray("outputParameters") : null;
				}
				case SERVICE_REQUEST_POST -> {
					JSONObject theService = thingModelService.getThingService(m.getProductCode(), m.getModelIdentifier());

					yield theService != null ? theService.getJSONArray("inputParameters") : null;
				}
				case SERVICE_REQUEST_REPLY -> {
					JSONObject theService = thingModelService.getThingService(m.getProductCode(), m.getModelIdentifier());

					yield theService != null ? theService.getJSONArray("outputParameters") : null;
				}
				case EVENT_POST -> {
					JSONObject theEvent = thingModelService.getThingEvent(m.getProductCode(), m.getModelIdentifier());

					yield theEvent != null ? theEvent.getJSONArray("parameters") : null;
				}
				case EVENT_REPLY -> null;
				};

				if (parametersInThingModel != null) {
					List<MessageParameter> messageParameters = parametersInThingModel.stream().map(p -> {
						JSONObject parameterObj = (JSONObject) p;

						MessageParameter messageParameter = new MessageParameter();
						messageParameter.setIdentifier(parameterObj.getString("identifier"));
						messageParameter.setName(parameterObj.getString("name"));
						messageParameter.setDataTypeSpecs(parameterObj.getObject("dataType", DataTypeSpecs.class));
						messageParameter.setRequired(parameterObj.getBooleanValue("required"));
						messageParameter.setDesc(parameterObj.getString("desc"));

						return messageParameter;
					}).toList();

					messageSpec.setParameters(messageParameters);
				}

				return messageSpec;
			}).toList();
		}

		ThingshubMessage replyMessage = new ThingshubMessage();
		replyMessage.setId(messageId);
		replyMessage.setClientId(Broker.getConsistentId());
		replyMessage.setVersion("1.0.0");
		replyMessage.setName(SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_MESSAGE_DEFINITION_ACK);
		replyMessage.setTime(DateUtil.now());
		replyMessage.setCode(MessageResult.SUCCESS.code());
		replyMessage.setMessage(MessageResult.SUCCESS.desc());
		replyMessage.setData(messageSpecs);

		createPublication4Reply(topic4Reply, replyMessage, publication.getExpireTime());
	}

	private void handleDeviceQuery(Publication publication) {
		JSONObject stdMessage = JSON.parseObject(publication.getPayload());
		String messageId = stdMessage.getString("id");
		String clientId = stdMessage.getString("clientId");
		JSONObject params = stdMessage.getJSONObject("params");

		JSONObject queryCriterions = params.getJSONObject("queryCriterions");
		String topic4Reply = String.format(SERVICE_CLIENT_INTERNAL_TOPIC_QUERY_DEVICE_REPLY, publication.getUsername(), clientId);

		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("tenantId", queryCriterions.getString("tenantId"));
		queryParams.put("customerId", queryCriterions.getString("customerId"));
		queryParams.put("region", queryCriterions.getString("region"));
		queryParams.put("group", queryCriterions.getString("group"));
		queryParams.put("productCode", queryCriterions.getString("productCode"));
		queryParams.put("sn", queryCriterions.getString("sn"));
		Page<Device> devices = deviceService.queryDevices(queryParams, queryCriterions.getIntValue("page"), queryCriterions.getIntValue("size"));

		ThingshubMessage replyMessage = new ThingshubMessage();
		replyMessage.setId(messageId);
		replyMessage.setClientId(Broker.getConsistentId());
		replyMessage.setVersion("1.0.0");
		replyMessage.setName(SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_DEVICE_ACK);
		replyMessage.setTime(DateUtil.now());
		replyMessage.setCode(MessageResult.SUCCESS.code());
		replyMessage.setMessage(MessageResult.SUCCESS.desc());
		replyMessage.setData(devices);

		createPublication4Reply(topic4Reply, replyMessage, publication.getExpireTime());
	}

	private void handleDeviceInfo(Publication publication) {
		JSONObject stdMessage = JSON.parseObject(publication.getPayload());
		String messageId = stdMessage.getString("id");
		String clientId = stdMessage.getString("clientId");
		JSONObject params = stdMessage.getJSONObject("params");
		String productCode = params.getString("productCode");
		String sn = params.getString("sn");
		String topic4Reply = String.format(SERVICE_CLIENT_INTERNAL_TOPIC_DEVICE_INFO_REPLY, publication.getUsername(), clientId);

		Device device = deviceService.getByProductSn(productCode, sn);

		ThingshubMessage replyMessage = new ThingshubMessage();
		replyMessage.setId(messageId);
		replyMessage.setClientId(Broker.getConsistentId());
		replyMessage.setVersion("1.0.0");
		replyMessage.setName(SERVICE_CLIENT_INTERNAL_MESSAGE_DEVICE_INFO_ACK);
		replyMessage.setTime(DateUtil.now());
		replyMessage.setCode(MessageResult.SUCCESS.code());
		replyMessage.setMessage(MessageResult.SUCCESS.desc());
		replyMessage.setData(device);

		createPublication4Reply(topic4Reply, replyMessage, publication.getExpireTime());
	}
}
