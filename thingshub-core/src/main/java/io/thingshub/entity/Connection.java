package io.thingshub.entity;

import java.util.Date;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.service.base.DataRegion;
import lombok.Data;

/**
 * <p>
 * Connection State Data
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Data
@DataRegion(name = "transient_region", persistent = false, local = false)
public class Connection {

	/**
	 * Tenant ID
	 */
	@QuerySqlField(index = true, name = "tenant_id", notNull = true)
	private String tenantId;

	/**
	 * Client ID
	 */
	@QuerySqlField(index = true, name = "client_id", notNull = true)
	private String clientId;

	/**
	 * Client Type
	 * <ul>
	 * <li>1-Device</li>
	 * <li>2-Client User</li>
	 * </ul>
	 */
	@QuerySqlField(name = "client_type", notNull = true)
	private Integer clientType;

	/**
	 * User Name
	 */
	@QuerySqlField(index = true, name = "user_name", notNull = true)
	private String username;

	/**
	 * Netty Channel ID
	 */
	@QuerySqlField(name = "channel_id", notNull = true)
	private String channelId;

	/**
	 * Client IP Address
	 */
	@QuerySqlField(name = "client_addr", notNull = true)
	private String clientAddr;

	/**
	 * Thingshub Server Node ID
	 */
	@QuerySqlField(index = true, name = "server_node", notNull = true)
	private String serverNode;

	/**
	 * Thingshub Server IP Address
	 */
	@QuerySqlField(name = "server_addr", notNull = true)
	private String serverAddr;

	/**
	 * Thingshub Server Name
	 */
	@QuerySqlField(name = "server_name", notNull = true)
	private String serverName;

	/**
	 * Transport Protocol
	 */
	@QuerySqlField
	private String transport;

	/**
	 * Connect Time
	 */
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@QuerySqlField(name = "connect_time", notNull = true)
	private Date connectTime;

}
