package io.thingshub.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import cn.hutool.db.sql.Condition;
import io.thingshub.commons.ServiceException;
import io.thingshub.commons.model.Page;
import io.thingshub.entity.ProductCat;
import io.thingshub.ioc.Service;
import io.thingshub.service.base.BaseService;
import io.thingshub.service.base.IdGenerator;
import jakarta.inject.Inject;

/**
 * <p>
 * 产品类别服务
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class ProductCatService extends BaseService<Long, ProductCat> {

	@Inject
	private IdGenerator idGenerator;

	public Page<ProductCat> queryProductCats(Map<String, Object> params, int page, int size) {
		List<Condition> conditions = params.entrySet().stream().map(entry -> {
			// TODO key到col 名称的映射
			return new Condition(entry.getKey(), entry.getValue());
		}).collect(Collectors.toList());
		return this.query(conditions, page, size);
	}

	public ProductCat getProductCat(Long id) {
		ProductCat productCat = this.getByKey(id);
		if (productCat != null && productCat.getDeletedStatus() != DeletedStatus.DELETED.value()) {
			return productCat;
		} else {
			return null;
		}
	}

	public Long createProductCat(ProductCat productCat) throws ServiceException {
		List<Condition> queryCondtions = Lists.newArrayList(new Condition("name", productCat.getName()),
				new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));
		ProductCat theCat = this.getOne(queryCondtions);
		if (theCat != null) {
			throw new ServiceException("品类名称已存在");
		}

		long id = idGenerator.nextId();
		productCat.setId(id);
		this.save(id, productCat);

		return id;
	}

	public void updateProductCat(ProductCat productCat) throws ServiceException {
		List<Condition> queryCondtions = Lists.newArrayList(new Condition("name", productCat.getName()),
				new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));
		ProductCat theCat = this.getOne(queryCondtions);
		if (theCat != null && !theCat.getId().equals(productCat.getId())) {
			throw new ServiceException("品类名称已存在");
		}

		this.save(productCat.getId(), productCat);
	}

}
