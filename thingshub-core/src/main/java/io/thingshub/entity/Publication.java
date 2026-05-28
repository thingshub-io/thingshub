package io.thingshub.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.commons.LongIdWriter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * Publishing Record
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class Publication implements Serializable {

	private static final long serialVersionUID = 7433413039128565398L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * Publisher User Name
	 */
	@QuerySqlField(index = true, notNull = true)
	private String username;

	/**
	 * Publisher Client ID
	 */
	@QuerySqlField(index = true, name = "client_id", notNull = true)
	private String clientId;

	/**
	 * Publish Way
	 * <ul>
	 * <li>1-Client Publish Directly</li>
	 * <li>2-Client Publish by Last Will</li>
	 * </ul>
	 */
	@QuerySqlField(name = "pub_way", notNull = true)
	private Integer pubWay;

	/**
	 * Publish Topic
	 */
	@QuerySqlField(index = true, name = "topic", notNull = true)
	private String topic;

	/**
	 * To Thingshub's Standard Topic
	 */
	@QuerySqlField(index = true, name = "std_topic", notNull = true)
	private String stdTopic;

	/**
	 * Message Properties
	 */
	@QuerySqlField
	private Map<String, Object> props;

	/**
	 * Standard Payload
	 */
	@QuerySqlField(name = "std_payload")
	private String stdPayload;

	/**
	 * Payload
	 */
	@QuerySqlField(name = "payload")
	private String payload;

	/**
	 * Expire Time
	 */
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@QuerySqlField(name = "expire_time")
	private Date expireTime;

	/**
	 * Create Time
	 */
	@QuerySqlField(index = true, notNull = true)
	private Long timestamp;

}
