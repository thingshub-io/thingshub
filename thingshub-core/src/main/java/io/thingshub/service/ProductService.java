package io.thingshub.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import cn.hutool.db.sql.Condition;
import io.thingshub.commons.Page;
import io.thingshub.commons.ServiceException;
import io.thingshub.entity.Product;
import io.thingshub.ioc.Service;
import io.thingshub.service.base.BaseService;
import io.thingshub.service.base.IdGenerator;
import jakarta.inject.Inject;

/**
 * <p>
 * 产品管理
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class ProductService extends BaseService<Long, Product> {

	@Inject
	private IdGenerator idGenerator;

	public Product getProduct(Long id) {
		Product product = this.getByKey(id);
		if (product != null && product.getDeletedStatus() != DeletedStatus.DELETED.value()) {
			return product;
		} else {
			return null;
		}
	}

	public Product getByCode(String code) {
		return this.getOne(Lists.newArrayList(new Condition("code", code), new Condition("deleted_status", DeletedStatus.NOT_DELETED.value())));
	}

	public List<Product> getProducts() {
		List<Condition> conditions = Lists.newArrayList(new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));

		return this.query(conditions);
	}

	public Page<Product> queryProducts(Map<String, Object> params, int page, int size) {
		List<Condition> conditions = params.entrySet().stream().map(entry -> {
			// TODO key到col 名称的映射
			return new Condition(entry.getKey(), entry.getValue());
		}).collect(Collectors.toList());
		conditions.add(new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));

		return this.query(conditions, page, size);
	}

	public Long createProduct(Product product) throws ServiceException {
		List<Condition> queryConditions = Lists.newArrayList(new Condition("code", product.getCode()),
				new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));
		if (this.getOne(queryConditions) != null) {
			throw new ServiceException("产品编号已存在");
		}

		long id = idGenerator.nextId();
		product.setId(id);
		this.save(id, product);

		return id;
	}

	public void updateProduct(Product product) throws ServiceException {
		List<Condition> queryConditions = Lists.newArrayList(new Condition("code", product.getCode()),
				new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));
		Product prod = this.getOne(queryConditions);
		if (prod != null && !prod.getId().equals(product.getId())) {
			throw new ServiceException("产品编号已存在");
		}

		this.save(product.getId(), product);
	}

	public void updateStatus(Long id, AvailableStatus status) {
		Product product = this.getByKey(id);
		if (product != null) {
			product.setStatus(status.value());

			this.save(id, product);
		}
	}

	public void remove(Long id) {
		Product product = this.getByKey(id);
		if (product != null) {
			product.setDeletedStatus(DeletedStatus.DELETED.value());

			this.save(id, product);
		}
	}

}
