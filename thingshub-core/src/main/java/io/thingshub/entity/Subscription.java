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
 * Client's Subscription
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class Subscription implements Serializable {

	private static final long serialVersionUID = -4734745094213474253L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * Subscriber's Client ID
	 */
	@QuerySqlField(index = true, name = "subscriber_id", notNull = true, orderedGroups = { @QuerySqlField.Group(name = "subscriber_topic_idx", order = 1) })
	private String subscriberId;

	/**
	 * Subscribe Group
	 */
	@QuerySqlField(index = true, name = "sub_group")
	private String group;

	/**
	 * Original Topic
	 */
	@QuerySqlField(index = true, name = "topic_filter", orderedGroups = { @QuerySqlField.Group(name = "subscriber_topic_idx", order = 2) })
	private String topicFilter;

	/**
	 * Standard Topic
	 */
	@QuerySqlField(index = true, name = "std_topic", notNull = true)
	private String stdTopic;

	/**
	 * Subscription's Properties
	 */
	@QuerySqlField
	private Map<String, Object> props;

	/**
	 * Subscribe Time
	 */
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@QuerySqlField(index = true, notNull = true)
	private Date time;

}
