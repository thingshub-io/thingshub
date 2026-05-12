package io.thingshub.transport;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;

import cn.hutool.db.sql.Condition;
import io.thingshub.Broker;
import io.thingshub.entity.Session;
import io.thingshub.ioc.Service;
import io.thingshub.service.InboxService;
import io.thingshub.service.SessionService;
import io.thingshub.service.base.IdGenerator;
import io.thingshub.subscribe.SubscriptionManager;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SessionManager {

	@Inject
	private SessionService sessionService;

	@Inject
	private InboxService inboxService;

	@Inject
	private SubscriptionManager subscriptionManager;

	@Inject
	private ConnectionManager connStateManager;

	@Inject
	private IdGenerator idGenerator;

	@PostConstruct
	public void init() {
		sessionService.listen((eventType, session) -> {
			switch (eventType) {
			case UPDATED -> {
				if (session.getExpireTime() != null) {
					log.info("Client [{}] is inactive and set expiry interval of subscriptions", session.getClientId());

					subscriptionManager.setExpiryInterval(session.getClientId(), session.getExpiryInterval().longValue(), TimeUnit.SECONDS);
				}
			}
			case EXPIRED -> {
				log.info("Client [{}] session expired", session.getClientId());
				sessionService.remove(Lists.newArrayList(new Condition("client_id", session.getClientId())));
			}
			case REMOVED -> {
				log.info("Client [{}] session removed", session.getClientId());

				inboxService.cleanDeliveries(session.getClientId());
				subscriptionManager.cleanSubscriptions(session.getClientId());
			}
			default -> {
			}
			}
		});

		Broker.registerNodeFailureHandler(this::handleSessionStateAfterNodeFailure);
	}

	public long create(String clientId, String nodeId, int expiryInterval) {
		long id = idGenerator.nextId();

		Session clientSession = new Session();
		clientSession.setId(id);
		clientSession.setClientId(clientId);
		clientSession.setExpiryInterval(expiryInterval);
		clientSession.setActiveTime(new Date());
		clientSession.setActivatedAt(nodeId);
		clientSession.setCreateTime(new Date());

		sessionService.save(id, clientSession);

		log.info("Client [{}] create a new session", clientId);

		return id;
	}

	public boolean isSessionExists(Long sessionId) {
		return sessionService.exists(sessionId);
	}

	public Session getClientSession(String clientId) {
		return sessionService.getOne(Lists.newArrayList(new Condition("client_id", clientId)));
	}

	public long resume(String clientId, String newNodeId, int newExpiryInterval) {
		Session theSession = this.getClientSession(clientId);
		theSession.setExpiryInterval(newExpiryInterval);
		theSession.setExpireTime(null);
		theSession.setActiveTime(new Date());
		theSession.setActivatedAt(newNodeId);

		sessionService.save(theSession.getId(), theSession);

		return theSession.getId();
	}

	public void suspend(String clientId) {
		Session theSession = this.getClientSession(clientId);

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, theSession.getExpiryInterval().intValue());
		theSession.setExpireTime(calendar.getTime());
		sessionService.save(theSession.getId(), theSession, theSession.getExpiryInterval().longValue(), TimeUnit.SECONDS);
	}

	public void clean(String clientId) {
		Session theSession = this.getClientSession(clientId);
		if (theSession == null) {
			inboxService.cleanDeliveries(clientId);
			subscriptionManager.cleanSubscriptions(clientId);
		} else {
			sessionService.remove(Lists.newArrayList(new Condition("client_id", clientId)));
		}
	}

	public void handleSessionStateAfterNodeFailure(String nodeId) {
		connStateManager.cleanConnectionInfoInFailedNode(nodeId);

		List<Session> sessions = sessionService.queryLiveSessionsInNode(nodeId);
		sessions.forEach(s -> {
			if (s.getExpiryInterval().intValue() == 0) {
				sessionService.removeByKey(s.getId());
			} else {
				Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.SECOND, s.getExpiryInterval().intValue());
				s.setExpireTime(calendar.getTime());
				sessionService.save(s.getId(), s, s.getExpiryInterval().longValue(), TimeUnit.SECONDS);
			}
		});
	}

}
