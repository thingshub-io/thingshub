package io.thingshub.subscribe;

import static io.thingshub.commons.ThingshubConstants.THINGSHUB_CLIENT_ID_PLACEHOLDER;
import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_SERVICE_FUNCTION_CALL;
import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_SERVICE_REQUEST_REPLY;
import static io.thingshub.subscribe.TopicUtils.DELIMITER;
import static io.thingshub.subscribe.TopicUtils.UNORDERED_SHARE;
import static io.thingshub.subscribe.TopicUtils.isTopicMatch;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.ignite.Ignite;

import com.google.common.collect.Lists;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import cn.hutool.db.sql.Condition;
import io.thingshub.commons.MessageType;
import io.thingshub.entity.Device;
import io.thingshub.entity.GroupSelection;
import io.thingshub.entity.MessageDefinition;
import io.thingshub.entity.Subscription;
import io.thingshub.ioc.Component;
import io.thingshub.service.DeviceService;
import io.thingshub.service.GroupSelectionService;
import io.thingshub.service.GroupSelectionService.GroupSelectionKey;
import io.thingshub.service.MessageDefinitionService;
import io.thingshub.service.SubscriptionService;
import io.thingshub.service.SubscriptionService.SubKey;
import io.thingshub.service.base.IdGenerator;
import io.thingshub.service.model.ClientType;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Subscription Management
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Component
@Slf4j
public class SubscriptionManager {

	private final TopicTree<MatchedSubscriber> topicTree;// topic subscription tree for client subscription data in current node

	private final BloomFilter<String> topicBloomFilter;// topic bloom filter for client subscribed topics in current node

	@Inject
	private Ignite ignite;

	@Inject
	private IdGenerator idGenerator;

	@Inject
	private DeviceService deviceService;

	@Inject
	private MessageDefinitionService messageDefinitionService;

	@Inject
	private SubscriptionService subscriptionService;

	@Inject
	private GroupSelectionService groupSelectionService;

	public SubscriptionManager() {
		this.topicTree = new TopicTree<>();
		this.topicBloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), 10_000_000, 0.01);
	}

	@PostConstruct
	public void init() {
		List<Subscription> subsInLocal = subscriptionService.listInLocal();
		if (subsInLocal != null) {
			subsInLocal.forEach(s -> {
				MatchedSubscriber match = new MatchedSubscriber(s.getSubscriberId(), s.getTopicFilter(), s.getProps(), s.getGroup());

				topicTree.add(s.getStdTopic(), match);
				topicBloomFilter.put(s.getStdTopic());

				if (s.getGroup() != null) {
					topicTree.add(UNORDERED_SHARE.concat(DELIMITER).concat(s.getGroup()), match);
					topicBloomFilter.put(UNORDERED_SHARE.concat(DELIMITER).concat(s.getGroup()));
				}
			});
		}

		subscriptionService.listenInLocal((et, s) -> {
			MatchedSubscriber subscriber = new MatchedSubscriber(s.getSubscriberId(), s.getTopicFilter(), s.getProps(), s.getGroup());

			switch (et) {
			case CREATED, UPDATED -> {
				topicTree.add(s.getStdTopic(), subscriber);
				topicBloomFilter.put(s.getStdTopic());

				if (s.getGroup() != null) {
					topicTree.add(UNORDERED_SHARE.concat(DELIMITER).concat(s.getGroup()), subscriber);
					topicBloomFilter.put(UNORDERED_SHARE.concat(DELIMITER).concat(s.getGroup()));
				}
			}
			case EXPIRED -> {
				topicTree.remove(s.getStdTopic(), subscriber);
				// 如果客户端频繁断开连接、取消订阅，会导致误判率增加。是否重建BloomFilter？？？

				if (s.getGroup() != null) {
					topicTree.remove(UNORDERED_SHARE.concat(DELIMITER).concat(s.getGroup()), subscriber);
				}

				subscriptionService.removeByKey(new SubKey(s.getId(), s.getSubscriberId()));
			}
			case REMOVED -> {

			}
			}
		});
	}

	public void setExpiryInterval(String subscriberId, long duration, TimeUnit timeUnit) {
		List<Subscription> subscribings = subscriptionService.query(Lists.newArrayList(new Condition("subscriber_id", subscriberId)));
		if (subscribings != null) {
			subscribings.forEach(s -> {
				SubKey subKey = new SubKey(s.getId(), s.getSubscriberId());
				subscriptionService.save(subKey, s, duration, timeUnit);
			});
		}
	}

	public Set<MatchedSubscriber> match(String topic) {
		return topicTree.match(topic);
	}

	public boolean hasSubscriberInLocal(String topic) {
		if (!topicBloomFilter.mightContain(topic)) {
			return false;
		}

		Set<MatchedSubscriber> matches = topicTree.match(topic);

		return !matches.isEmpty();
	}

	public Boolean hasMatch(String topic) {
		return ignite.compute().execute(TopicMatchingChecker.class, topic);
	}

	public boolean hasSubscribed(String clientId, String group, String topicFilter) {
		boolean hasSubscribed = false;
		List<Condition> conditions = Lists.newArrayList(new Condition("subscriber_id", clientId), new Condition("sub_group", group), new Condition("topic_filter", topicFilter));
		List<Subscription> subscribings = subscriptionService.query(conditions);
		if (subscribings != null && subscribings.size() > 0) {
			hasSubscribed = true;
		}

		return hasSubscribed;
	}

	public void subscribe(ClientSubscribe clientSubscribe) {
		List<String> topicFilters = Lists.newArrayList();

		if (clientSubscribe.getClientType() == ClientType.DEVICE_CLIENT) {
			Device device = deviceService.getBySn(clientSubscribe.getClientId());
			String productCode = device.getProductCode();
			String subscriberId = clientSubscribe.getClientId();

			List<MessageDefinition> downwardMessages = messageDefinitionService.getDownwardMessages(device.getProductCode());
			if (downwardMessages != null) {
				downwardMessages.forEach(m -> {
					String msgTopic = m.getTopic().replace(THINGSHUB_CLIENT_ID_PLACEHOLDER, device.getSn());
					if (isTopicMatch(clientSubscribe.getTopicFilter(), msgTopic)) {
						String topicFilter = switch (MessageType.of(m.getType())) {
						case SERVICE_FUNCTION_CALL -> String.format(THING_TOPIC_SERVICE_FUNCTION_CALL, productCode, subscriberId, m.getName());
						case SERVICE_REQUEST_REPLY -> String.format(THING_TOPIC_SERVICE_REQUEST_REPLY, productCode, subscriberId, m.getName());
						default -> null;
						};

						if (topicFilter != null) {
							topicFilters.add(topicFilter);
						}
					}
				});
			}

			if (topicFilters.size() == 0) {
				log.warn("SUBSCRIBE: no matched topic in device's message definition, sn: {} ", device.getSn());
			}
		} else if (clientSubscribe.getClientType() == ClientType.SERVICE_CLIENT) {
			topicFilters.add(clientSubscribe.getTopicFilter());
		}

		topicFilters.forEach(tf -> {
			long subcriptionId = idGenerator.nextId();
			Subscription subcription = new Subscription();
			subcription.setId(subcriptionId);
			subcription.setSubscriberId(clientSubscribe.getClientId());
			subcription.setGroup(clientSubscribe.getGroup());
			subcription.setTopicFilter(clientSubscribe.getTopicFilter());
			subcription.setStdTopic(tf);
			subcription.setProps(clientSubscribe.getProps());
			subcription.setTime(new Date());

			SubKey subKey = new SubKey(subcriptionId, clientSubscribe.getClientId());
			subscriptionService.save(subKey, subcription);
		});
	}

	public boolean unsubscribe(ClientUnsubscribe clientUnsubscribe) {
		boolean hasSubscribed = false;
		List<Condition> conditions = Lists.newArrayList(new Condition("subscriber_id", clientUnsubscribe.getClientId()), new Condition("sub_group", clientUnsubscribe.getGroup()),
				new Condition("topic_filter", clientUnsubscribe.getTopicFilter()));
		List<Subscription> subcribings = subscriptionService.query(conditions);
		if (subcribings != null && subcribings.size() > 0) {
			hasSubscribed = true;
			subscriptionService.remove(conditions);
		}

		return hasSubscribed;
	}

	public void cleanSubscriptions(String subscriberId) {
		subscriptionService.remove(Lists.newArrayList(new Condition("subscriber_id", subscriberId)));
	}

	public List<MatchedSubscriber> getTopicMatchesInGroup(String group) {
		Set<MatchedSubscriber> matchesInGroup = topicTree.match(UNORDERED_SHARE.concat(DELIMITER).concat(group));

		return matchesInGroup.stream().toList();
	}

	public boolean confirmMessageReceiver(Long publicationId, String group, String subscriberId) {
		GroupSelection groupSelection = new GroupSelection();
		groupSelection.setDataId(publicationId.toString());
		groupSelection.setGroup(group);
		groupSelection.setWinner(subscriberId);
		groupSelection.setCreateTime(new Date());
		GroupSelectionKey theKey = new GroupSelectionKey(publicationId.toString(), group);

		return groupSelectionService.saveIfAbsent(theKey, groupSelection, 10 * 60, SECONDS);
	}

	public List<String> getMappedStdTopics(String subscriberId, String topicFilter) {
		List<String> stdTopics = Lists.newArrayList();
		List<Condition> conditions = Lists.newArrayList(new Condition("subscriber_id", subscriberId), new Condition("topic_filter", topicFilter));

		List<Subscription> subscribings = subscriptionService.query(conditions);
		if (subscribings != null && subscribings.size() > 0) {
			stdTopics = subscribings.stream().map(s -> s.getStdTopic()).toList();
		}

		return stdTopics;
	}

}
