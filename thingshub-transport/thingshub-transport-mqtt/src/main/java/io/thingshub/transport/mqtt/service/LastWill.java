package io.thingshub.transport.mqtt.service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.netty.handler.codec.mqtt.MqttProperties.StringPair;
import io.thingshub.commons.LongIdWriter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * Last Will Message
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class LastWill implements Serializable {

	private static final long serialVersionUID = 7433413039128565398L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * User Name
	 */
	@QuerySqlField(name = "user_name", index = true, notNull = true)
	private String username;

	/**
	 * Client ID
	 */
	@QuerySqlField(name = "client_id", index = true, notNull = true)
	private String clientId;

	/**
	 * Client Type
	 * <ul>
	 * <li>1-Device Client</li>
	 * <li>2-Service Client</li>
	 * </ul>
	 */
	@QuerySqlField(name = "client_type", notNull = true)
	private Integer clientType;

	/**
	 * Product Code. If client type is device, not null
	 */
	@QuerySqlField(name = "product_code")
	private String productCode;

	/**
	 * Associated Session ID
	 */
	@QuerySqlField(name = "session_id", index = true, notNull = true)
	private Long sessionId;

	/**
	 * Topic
	 */
	@QuerySqlField(notNull = true)
	private String topic;

	/**
	 * Qos
	 */
	@QuerySqlField(notNull = true)
	private Integer qos;

	/**
	 * Retain Flag
	 */
	@QuerySqlField(notNull = true)
	private Boolean retain;

	/**
	 * Payload
	 */
	@QuerySqlField
	private String payload;

	/**
	 * Expiry Interval
	 */
	@QuerySqlField(name = "expiry_interval", notNull = true)
	private Integer expiryInterval;

	/**
	 * Payload Format Indicator Flag
	 */
	@QuerySqlField(name = "payload_format_indicator")
	private Boolean payloadFormatIndicator;

	/**
	 * Content Type
	 */
	@QuerySqlField(name = "content_type")
	private String contentType;

	/**
	 * Response Topic
	 */
	@QuerySqlField(name = "response_topic")
	private String responseTopic;

	/**
	 * Correlation Data
	 */
	@QuerySqlField(name = "correlation_data")
	private String correlationData;

	/**
	 * User Properties
	 */
	@QuerySqlField(name = "user_properties")
	private List<StringPair> userProperties;

	/**
	 * Create Time
	 */
	@QuerySqlField(name = "create_time", notNull = true)
	private Date createTime;

}
