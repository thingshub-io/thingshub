package io.thingshub.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.db.sql.Condition;
import io.thingshub.commons.Page;
import io.thingshub.entity.Publication;
import io.thingshub.ioc.Service;
import io.thingshub.service.base.BaseService;

/**
 * <p>
 * Publishing Record Management
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class PublicationService extends BaseService<Long, Publication> {

	public Page<Publication> queryPublications(Map<String, Object> params, int page, int size) {
		List<Condition> conditions = params.entrySet().stream().map(entry -> {
			// TODO key到col 名称的映射
			return new Condition(entry.getKey(), entry.getValue());
		}).collect(Collectors.toList());

		return this.query(conditions, page, size);
	}

}