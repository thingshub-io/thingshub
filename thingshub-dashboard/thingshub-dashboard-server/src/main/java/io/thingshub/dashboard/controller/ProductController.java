package io.thingshub.dashboard.controller;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import io.thingshub.commons.IdParam;
import io.thingshub.commons.ItemOption;
import io.thingshub.commons.LongId;
import io.thingshub.commons.ServiceException;
import io.thingshub.commons.model.Page;
import io.thingshub.commons.model.ProductLinkMode;
import io.thingshub.commons.model.ProductType;
import io.thingshub.dashboard.params.ProductRequestParams.ProductCatFormParams;
import io.thingshub.dashboard.params.ProductRequestParams.ProductFormParams;
import io.thingshub.dashboard.params.ProductRequestParams.QueryProductCatParams;
import io.thingshub.dashboard.params.ProductRequestParams.QueryProductParams;
import io.thingshub.dashboard.server.UserContextHolder;
import io.thingshub.dashboard.server.UserInfo;
import io.thingshub.entity.Product;
import io.thingshub.entity.ProductCat;
import io.thingshub.http.HttpMethod;
import io.thingshub.http.annotation.Controller;
import io.thingshub.http.annotation.RequestBody;
import io.thingshub.http.annotation.RequestMapping;
import io.thingshub.http.annotation.RequestParam;
import io.thingshub.service.ProductCatService;
import io.thingshub.service.ProductService;
import io.thingshub.service.base.BaseService.AvailableStatus;
import io.thingshub.service.base.BaseService.DeletedStatus;
import io.thingshub.transport.ContentType;
import io.thingshub.transport.Transport;
import jakarta.inject.Inject;

/**
 * <p>
 * 产品管理
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Controller
public class ProductController {

	@Inject
	private ProductCatService productCatService;

	@Inject
	private ProductService productService;

	@RequestMapping(path = "/product/cat/options")
	public List<ItemOption> listProductCatOptions() {
		List<ItemOption> options = Collections.emptyList();

		List<ProductCat> productCats = productCatService.list();
		if (productCats != null) {
			options = productCats.stream().filter(cat -> cat.getDeletedStatus() == DeletedStatus.NOT_DELETED.value())
					.map(cat -> new ItemOption(cat.getId() + "", cat.getName())).toList();
		}

		return options;
	}

	@RequestMapping(path = "/product/cat/query")
	public Page<ProductCat> queryProductCat(QueryProductCatParams params) {
		Map<String, Object> queryParams = new HashMap<>();

		return productCatService.queryProductCats(queryParams, params.getPage(), params.getSize());
	}

	@RequestMapping(path = "/product/cat/info")
	public ProductCat getProductCat(@RequestParam Long id) {
		return productCatService.getProductCat(id);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/product/cat/save")
	public LongId saveProductCat(@RequestBody ProductCatFormParams params) throws ServiceException {
		UserInfo userInfo = UserContextHolder.getCurrentUser();
		Long catId = params.getId();
		if (catId == null) {
			ProductCat productCat = new ProductCat();
			productCat.setName(params.getName());
			productCat.setRemark(params.getRemark());
			productCat.setDeletedStatus(DeletedStatus.NOT_DELETED.value());
			productCat.setCreateBy(userInfo.getName());
			productCat.setCreateTime(new Date());
			catId = productCatService.createProductCat(productCat);
		} else {
			ProductCat productCat = productCatService.getProductCat(params.getId());
			if (productCat == null) {
				throw new ServiceException("无效的产品品类ID");
			}

			productCat.setName(params.getName());
			productCat.setRemark(params.getRemark());
			productCatService.updateProductCat(productCat);
		}

		return new LongId(catId);
	}

	@RequestMapping(path = "/product/type/options")
	public List<ItemOption> listProductTypeOptions() {
		List<ItemOption> options = Lists.newArrayList();

		for (ProductType productType : ProductType.values()) {
			options.add(new ItemOption(productType.type() + "", productType.title()));
		}

		return options;
	}

	@RequestMapping(path = "/product/link-mode/options")
	public List<ItemOption> listProductLinkModeOptions() {
		List<ItemOption> options = Lists.newArrayList();

		for (ProductLinkMode linkMode : ProductLinkMode.values()) {
			options.add(new ItemOption(linkMode.type() + "", linkMode.title()));
		}

		return options;
	}

	@RequestMapping(path = "/product/content-type/options")
	public List<ItemOption> listProductContentTypeOptions() {
		List<ItemOption> options = Lists.newArrayList();

		for (ContentType contentType : ContentType.values()) {
			options.add(new ItemOption(contentType.name(), contentType.title()));
		}

		return options;
	}

	@RequestMapping(path = "/product/transport/options")
	public List<ItemOption> listProductTransportOptions() {
		List<ItemOption> options = Lists.newArrayList();

		for (Transport transport : Transport.values()) {
			options.add(new ItemOption(transport.name() + "", transport.title()));
		}

		return options;
	}

	@RequestMapping(path = "/product/options")
	public List<ItemOption> listProductOptions() {
		List<ItemOption> options = Collections.emptyList();

		List<Product> products = productService.list();
		if (products != null) {
			options = products.stream().map(p -> new ItemOption(p.getCode(), p.getName())).toList();
		}

		return options;
	}

	@RequestMapping(path = "/product/query")
	public Page<Product> queryProduct(QueryProductParams params) {
		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("code", params.getCode());
		queryParams.put("name", params.getName());

		return productService.queryProducts(queryParams, params.getPage(), params.getSize());
	}

	@RequestMapping(path = "/product/info")
	public Product getProduct(@RequestParam Long id) {
		return productService.getProduct(id);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/product/save")
	public LongId saveProduct(@RequestBody ProductFormParams params) throws ServiceException {
		UserInfo userInfo = UserContextHolder.getCurrentUser();
		ProductCat productCat = productCatService.getProductCat(params.getCatId());
		if (productCat == null) {
			throw new ServiceException("无效的产品品类");
		}

		Long productId = params.getId();
		if (productId == null) {
			Product product = new Product();
			BeanUtil.copyProperties(params, product, CopyOptions.create().setIgnoreNullValue(true));

			product.setCatName(productCat.getName());
			product.setStatus(AvailableStatus.NORMAL.value());
			product.setDeletedStatus(DeletedStatus.NOT_DELETED.value());
			product.setCreateBy(userInfo.getName());
			product.setCreateTime(new Date());
			productId = productService.createProduct(product);
		} else {
			Product product = productService.getProduct(params.getId());
			if (product == null) {
				throw new ServiceException("无效的产品ID");
			}

			BeanUtil.copyProperties(params, product, CopyOptions.create().setIgnoreNullValue(true));
			product.setCatName(productCat.getName());
			productService.updateProduct(product);
		}

		return new LongId(productId);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/product/disable")
	public void disableProduct(@RequestBody IdParam idParam) {
		productService.updateStatus(idParam.getId(), AvailableStatus.DISABLED);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/product/enable")
	public void enableProduct(@RequestBody IdParam idParam) {
		productService.updateStatus(idParam.getId(), AvailableStatus.NORMAL);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/product/remove")
	public void removeProduct(@RequestBody IdParam idParam) {
		productService.remove(idParam.getId());
		// TODO 删除所有的设备？？？
	}

}