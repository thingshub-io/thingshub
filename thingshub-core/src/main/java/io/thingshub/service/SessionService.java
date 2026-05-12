package io.thingshub.service;

import static org.apache.ignite.cache.query.IndexQueryCriteriaBuilder.eq;

import java.util.Collections;
import java.util.List;

import javax.cache.Cache.Entry;

import org.apache.ignite.cache.query.IndexQuery;
import org.apache.ignite.cache.query.Query;

import io.thingshub.entity.Session;
import io.thingshub.ioc.Service;
import io.thingshub.service.base.BaseService;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Session State Service
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
@Slf4j
public class SessionService extends BaseService<Long, Session> {

	public List<Session> queryLiveSessionsInNode(String nodeId) {
		try {
			Query<Entry<Long, Session>> indexQuery = new IndexQuery<Long, Session>(Session.class, "activated_at") //
					.setCriteria(eq("activated_at", nodeId)) //
					.setFilter((id, s) -> s.getExpireTime() == null);
			List<Entry<Long, Session>> sessionEntries = this.entityCache.query(indexQuery).getAll();

			return sessionEntries.stream().map(entry -> entry.getValue()).toList();
		} catch (Exception e) {
			log.error("", e);

			return Collections.emptyList();
		}
	}

}
