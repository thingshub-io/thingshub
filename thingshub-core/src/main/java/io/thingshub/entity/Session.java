package io.thingshub.entity;

import java.util.Date;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.commons.LongIdWriter;
import lombok.Data;

/**
 * <p>
 * Session State Data
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Data
public class Session {

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * Client ID
	 */
	@QuerySqlField(index = true, name = "client_id", notNull = true)
	private String clientId;

	/**
	 * Session Expiry Interval(Unit: second)
	 */
	@QuerySqlField(name = "expiry_interval", notNull = true)
	private Integer expiryInterval;

	/**
	 * Last Active Time
	 */
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@QuerySqlField(name = "active_time", notNull = true)
	private Date activeTime;

	/**
	 * At which node is session activated
	 */
	@QuerySqlField(index = true, name = "activated_at", notNull = true)
	private String activatedAt;

	/**
	 * Expire Time
	 */
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@QuerySqlField(name = "expire_time")
	private Date expireTime;

	/**
	 * Create Time
	 */
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@QuerySqlField(name = "create_time", notNull = true)
	private Date createTime;

}
