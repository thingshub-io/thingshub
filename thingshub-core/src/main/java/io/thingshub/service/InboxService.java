package io.thingshub.service;

import static org.apache.ignite.cache.query.IndexQueryCriteriaBuilder.eq;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.cache.Cache.Entry;

import org.apache.ignite.cache.query.IndexQuery;
import org.apache.ignite.cache.query.Query;

import com.google.common.collect.Lists;

import cn.hutool.db.sql.Condition;
import io.thingshub.commons.model.Page;
import io.thingshub.entity.Inbox;
import io.thingshub.ioc.Service;
import io.thingshub.service.base.BaseService;
import io.thingshub.service.base.IdGenerator;
import io.thingshub.service.model.Delivery;
import io.thingshub.transport.DeliverySource;
import io.thingshub.transport.DelvieryStatus;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Inbox Service
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
@Slf4j
public class InboxService extends BaseService<Long, Inbox> {

	@Inject
	private IdGenerator idGenerator;

	public Page<Delivery> queryDeliveries(Map<String, Object> params, int page, int size) {
		List<Condition> conditions = params.entrySet().stream().map(entry -> {
			// TODO key到col 名称的映射
			return new Condition(entry.getKey(), entry.getValue());
		}).collect(Collectors.toList());

		Page<Inbox> result = this.query(conditions, page, size);
		List<Delivery> deliveries = result.getRecords().stream()
				.map(m -> Delivery.builder().id(m.getId()).receiverId(m.getReceiverId()).recProps(m.getRecProps()).senderId(m.getSenderId()).sendProps(m.getSendProps())
						.topic(m.getTopic()).payload(m.getPayload()).deliverySource(DeliverySource.of(m.getDeliverySource())).deliverTime(m.getDeliverTime()).build())
				.toList();

		return Page.of(result.getCurrent(), result.getSize(), result.getTotal(), deliveries);
	}

	public List<Delivery> fetchOfflineDeliveries(String subscriberId, Date endTime, int size) {
		try {
			Query<Entry<Long, Inbox>> indexQuery = new IndexQuery<Long, Inbox>(Inbox.class, "rec_status_idx").setCriteria(eq("receiver_id", subscriberId), eq("status", 0)) //
					.setLimit(size);
			List<Entry<Long, Inbox>> deliveryEntries = this.entityCache.query(indexQuery).getAll();

			return deliveryEntries.stream()
					.map(entry -> Delivery.builder().id(entry.getValue().getId()).receiverId(entry.getValue().getReceiverId()).recProps(entry.getValue().getRecProps())
							.senderId(entry.getValue().getSenderId()).sendProps(entry.getValue().getSendProps()).topic(entry.getValue().getTopic())
							.payload(entry.getValue().getPayload()).deliverySource(DeliverySource.of(entry.getValue().getDeliverySource()))
							.deliverTime(entry.getValue().getDeliverTime()).build())
					.toList();
		} catch (Exception e) {
			log.error("", e);

			return Collections.emptyList();
		}
	}

	public void deliver(Delivery delivery, long duration, TimeUnit timeUnit) {
		Inbox inbox = new Inbox();
		inbox.setId(delivery.id());
		inbox.setReceiverId(delivery.receiverId());
		inbox.setRecProps(delivery.recProps());
		inbox.setSenderId(delivery.senderId());
		inbox.setSendProps(delivery.sendProps());
		inbox.setTopic(delivery.topic());
		inbox.setPayload(delivery.payload());
		inbox.setDeliverySource(delivery.deliverySource().value());
		inbox.setDeliverTime(delivery.deliverTime());

		Calendar calendar = Calendar.getInstance();
		switch (timeUnit) {
		case MILLISECONDS -> calendar.add(Calendar.MILLISECOND, (int) duration);
		case SECONDS -> calendar.add(Calendar.SECOND, (int) duration);
		case MINUTES -> calendar.add(Calendar.MINUTE, (int) duration);
		case HOURS -> calendar.add(Calendar.HOUR, (int) duration);
		case DAYS -> calendar.add(Calendar.DATE, (int) duration);
		default -> {
		}
		}
		inbox.setExpireTime(calendar.getTime());
		inbox.setStatus(DelvieryStatus.DELIVERED.value());

		this.save(inbox.getId(), inbox, duration, timeUnit);
	}

	public void ackedDelivery(Long deliveryId) {
		Inbox theDelivery = this.getByKey(deliveryId);
		if (theDelivery != null && theDelivery.getStatus() == DelvieryStatus.DELIVERED.value()) {
			theDelivery.setStatus(DelvieryStatus.ACKED.value());
			theDelivery.setAckTime(new Date());
			this.save(deliveryId, theDelivery);
		}
	}

	public void cleanDeliveries(String clientId) {
		this.remove(Lists.newArrayList(new Condition("client_id", clientId)));
	}

}