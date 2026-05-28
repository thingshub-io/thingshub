package io.thingshub.entity;

import java.io.Serializable;
import java.util.Date;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.commons.LongIdWriter;
import lombok.Data;

/**
 * <p>
 * Message Definition
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class MessageDefinition implements Serializable {

	private static final long serialVersionUID = 7720699406836904572L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * Message Name
	 */
	@QuerySqlField(index = true, notNull = true)
	private String name;

	/**
	 * Message Title
	 */
	@QuerySqlField(notNull = true)
	private String title;

	/**
	 * Product Code
	 */
	@QuerySqlField(index = true, name = "product_code", notNull = true)
	private String productCode;

	/**
	 * Message Type
	 * <ul>
	 * <li>PROPERTY_POST - Device Post Properties</li>
	 * <li>PROPERTY_REPLY - Reply for Device Post Properties</li>
	 * <li>SERVICE_FUNCTION_CALL - Call Device's Function</li>
	 * <li>SERVICE_FUNCTION_REPLY - Reply for Function Call</li>
	 * <li>SERVICE_REQUEST_POST - Device Post Request</li>
	 * <li>SERVICE_REQUEST_REPLY - Reply for Request Post</li>
	 * <li>EVENT_POST - Device Post Event</li>
	 * <li>EVENT_REPLY - Reply for Device Post Event</li>
	 * </ul>
	 */
	@QuerySqlField(notNull = true)
	private String type;

	/**
	 * Thing Model Identifier.
	 * <p/>
	 * Message parameters are referenced from input parameters or out parameters
	 * defined in thing model;
	 */
	@QuerySqlField(index = true, name = "model_identifier")
	private String modelIdentifier;

	/**
	 * Target Topic
	 */
	@QuerySqlField(notNull = true)
	private String topic;

	/**
	 * Description
	 */
	@QuerySqlField
	private String description;

	/**
	 * Delete Status
	 * <ul>
	 * <li>0-Not Deleted</li>
	 * <li>1-Deleted</li>
	 * </ul>
	 */
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
	@QuerySqlField(notNull = true)
	private String createBy;

}