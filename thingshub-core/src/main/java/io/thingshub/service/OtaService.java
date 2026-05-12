package io.thingshub.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.db.sql.Condition;
import io.thingshub.commons.model.Page;
import io.thingshub.entity.OtaPackage;
import io.thingshub.ioc.Service;
import io.thingshub.service.base.BaseService;

/**
 * <p>
 * Device's OTA Package
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class OtaService extends BaseService<Long, OtaPackage> {

	public Page<OtaPackage> queryOtaPackages(Map<String, Object> params, int page, int size) {
		List<Condition> conditions = params.entrySet().stream().map(entry -> {
			// TODO key到col 名称的映射
			return new Condition(entry.getKey(), entry.getValue());
		}).collect(Collectors.toList());
		conditions.add(new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));

		return this.query(conditions, page, size);
	}

}
