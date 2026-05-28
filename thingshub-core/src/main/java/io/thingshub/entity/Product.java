package io.thingshub.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.commons.LongIdWriter;
import lombok.Data;

/**
 * <p>
 * Product Info
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class Product implements Serializable {

	private static final long serialVersionUID = 5565453824453010319L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * Product Code
	 */
	@QuerySqlField(index = true, notNull = true)
	private String code;

	/**
	 * Product Name
	 */
	@QuerySqlField(notNull = true)
	private String name;

	/**
	 * Product Category ID
	 */
	@QuerySqlField(name = "cat_id", index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long catId;

	/**
	 * Product Category Name
	 */
	@QuerySqlField(name = "cat_name", notNull = true)
	private String catName;

	/**
	 * Product Type.
	 * <ul>
	 * <li>1-Direct Link Device</li>
	 * <li>2-Gateway Device;</li>
	 * <li>3-Gateway Sub Device</li>
	 * <li>4-Video Monitor Device</li>
	 * <li>9-Other</li>
	 * </ul>
	 */
	@QuerySqlField(notNull = true)
	private Integer type;

	/**
	 * Link Mode
	 * <ul>
	 * <li>1-Ethernet</li>
	 * <li>2-Cellular Network(3G/4G/5G)</li>
	 * <li>3-WIFI</li>
	 * <li>9-Other</li>
	 * </ul>
	 */
	@QuerySqlField(name = "link_mode", notNull = true)
	private Integer linkMode;

	/**
	 * Content Type. Such as JSON, binary, text etc.
	 */
	@QuerySqlField(name = "content_type", notNull = true)
	private String contentType;

	/**
	 * Transport Protocol. Such as TCP, MQTT, MQTT_WS, HTTP, UDP, GB28181 etc.
	 */
	@QuerySqlField(notNull = true)
	private String transport;

	/**
	 * Protocol Adaptor
	 */
	@QuerySqlField(name = "proto_adaptor")
	private String protoAdaptor;

	/**
	 * Product Image URLs
	 */
	@QuerySqlField
	private List<String> imgs;

	/**
	 * Firmware Id
	 */
	@QuerySqlField(name = "firmware_id")
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long firmwareId;

	/**
	 * Software Id
	 */
	@QuerySqlField(name = "software_id")
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long softwareId;

	/**
	 * Access Key
	 */
	@QuerySqlField(name = "access_key")
	private String accessKey;

	/**
	 * Access Secret
	 */
	@QuerySqlField(name = "access_secret")
	private String accessSecret;

	/**
	 * Remark
	 */
	@QuerySqlField
	private String remark;

	/**
	 * Available Status
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