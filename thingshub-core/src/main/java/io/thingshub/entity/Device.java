package io.thingshub.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.commons.LongIdWriter;
import lombok.Data;

/**
 * <p>
 * Device Info
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class Device implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * Device SN
	 */
	@QuerySqlField(index = true, notNull = true)
	private String sn;

	/**
	 * Device Group Id
	 */
	@QuerySqlField(index = true, name = "group_id", notNull = true)
	private Long groupId;

	/**
	 * Device Group Name
	 */
	@QuerySqlField(index = true, name = "group_name", notNull = true)
	private String groupName;

	/**
	 * Product Code
	 */
	@QuerySqlField(index = true, name = "product_code", notNull = true)
	private String productCode;

	/**
	 * Product Name
	 */
	@QuerySqlField(name = "product_name", notNull = true)
	private String productName;

	/**
	 * User Name
	 */
	@QuerySqlField(index = true)
	private String username;

	/**
	 * Password
	 */
	@QuerySqlField
	private String password;

	/**
	 * Protocol Version
	 */
	@QuerySqlField(name = "protocol_version")
	private String protocolVersion;

	/**
	 * Region Code
	 */
	@QuerySqlField(index = true, notNull = true)
	private String region;

	/**
	 * Address
	 */
	@QuerySqlField
	private String address;

	/**
	 * Latitude
	 */
	@QuerySqlField
	private BigDecimal lat;

	/**
	 * Longitude
	 */
	@QuerySqlField
	private BigDecimal lng;

	/**
	 * QR Code
	 */
	@QuerySqlField(name = "qr_code")
	private String qrCode;

	/**
	 * Tenant ID
	 */
	@QuerySqlField(index = true, name = "tenant_id")
	private String tenantId;

	/**
	 * Tenant Name
	 */
	@QuerySqlField(name = "tenant_name")
	private String tenantName;

	/**
	 * Customer ID
	 */
	@QuerySqlField(index = true, name = "customer_id")
	private String customerId;

	/**
	 * Customer Name
	 */
	@QuerySqlField(name = "customer_name")
	private String customerName;

	/**
	 * Connect State
	 * <ul>
	 * <li>0-Offline</li>
	 * <li>1-Online</li>
	 * </ul>
	 */
	@QuerySqlField(name = "connect_state", notNull = true)
	private Integer connectState;

	/**
	 * Fault State
	 * <ul>
	 * <li>0-Normal</li>
	 * <li>1-Fault</li>
	 * </ul>
	 */
	@QuerySqlField(name = "fault_state", notNull = true)
	private Integer faultState;

	/**
	 * Last Message Report Time
	 */
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@QuerySqlField(name = "report_time")
	private Date reportTime;

	/**
	 * Remark
	 */
	@QuerySqlField
	private String remark;

	/**
	 * Device Available Status
	 * <ul>
	 * <li>0-Enable</li>
	 * <li>1-Disable</li>
	 * </ul>
	 */
	@QuerySqlField(notNull = true)
	private Integer status;

	/**
	 * Delete Status
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
	 * Creator Name
	 */
	@QuerySqlField(name = "create_by", notNull = true)
	private String createBy;

}