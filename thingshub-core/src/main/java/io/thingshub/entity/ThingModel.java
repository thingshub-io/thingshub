package io.thingshub.entity;

import java.io.Serializable;
import java.util.Date;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.commons.LongIdWriter;
import lombok.Data;

/**
 * <p>
 * Device's Thing Model
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class ThingModel implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * Product Code
	 */
	@QuerySqlField(index = true, name = "product_code", notNull = true)
	private String productCode;

	/**
	 * Thing Model Specification
	 */
	@QuerySqlField
	private String specification;

	/**
	 * Deleted Status.。0-Not Deleted, 1-Deleted；
	 */
	@JSONField(serialize = false)
	@QuerySqlField(name = "deleted_status", notNull = true)
	private Integer deletedStatus;

	/**
	 * Create Time
	 */
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@QuerySqlField(index = true, name = "create_time", notNull = true)
	private Date createTime;

	/**
	 * Creator
	 */
	@QuerySqlField(name = "create_by", notNull = true)
	private String createBy;

}