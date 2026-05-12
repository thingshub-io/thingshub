package io.thingshub.transport.mqtt.service;

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
 * Retain Message
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class Retain implements Serializable {

	private static final long serialVersionUID = -8952257700697171258L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * Raw Topic
	 */
	@QuerySqlField(index = true, name = "topic", notNull = true)
	private String topic;

	/**
	 * Standard Topic
	 */
	@QuerySqlField(index = true, name = "std_topic", notNull = true)
	private String stdTopic;

	/**
	 * Message's Properties
	 */
	@QuerySqlField
	private Map<String, Object> props;

	/**
	 * Payload
	 */
	@QuerySqlField
	private String payload;

	/**
	 * Publisher's ID
	 */
	@QuerySqlField(name = "publisher_id")
	private String publisherId;

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
	@QuerySqlField(index = true, name = "create_time", notNull = true)
	private Date createTime;

}
