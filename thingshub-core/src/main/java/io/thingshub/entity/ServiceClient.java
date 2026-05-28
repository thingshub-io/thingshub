package io.thingshub.entity;

import java.io.Serializable;
import java.util.Date;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.commons.LongIdWriter;
import lombok.Data;

/**
 * <p>
 * Upstream Service Client
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class ServiceClient implements Serializable {

	private static final long serialVersionUID = -8225088207420924112L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * User Name Used by Upstream Service
	 */
	@QuerySqlField(index = true, notNull = true)
	private String username;

	/**
	 * Password
	 */
	@QuerySqlField(notNull = true)
	private String password;

	/**
	 * Upstream Service Name
	 */
	@QuerySqlField(name = "service_name")
	private String serviceName;

	/**
	 * Products Bound to Service
	 */
	@QuerySqlField(name = "product_codes")
	private String productCodes;

	/**
	 * remark
	 */
	@QuerySqlField
	private String remark;

	/**
	 * User Status
	 * <ul>
	 * <li>0-Enable</li>
	 * <li>1-Disable</li>
	 * </ul>
	 */
	@QuerySqlField(notNull = true)
	private Integer status;

	/**
	 * User's Deleted Status
	 * <ul>
	 * <li>0-Undeleted</li>
	 * <li>1-Deleted</li>
	 * </ul>
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
	 * Creator's Name
	 */
	@QuerySqlField(name = "create_by", notNull = true)
	private String createBy;

}