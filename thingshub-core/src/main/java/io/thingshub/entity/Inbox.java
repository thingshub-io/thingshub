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
 * Client's Inbox
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class Inbox implements Serializable {

	private static final long serialVersionUID = 7103406945700321832L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * Receiver ID
	 */
	@QuerySqlField(index = true, name = "receiver_id", notNull = true, orderedGroups = { @QuerySqlField.Group(name = "rec_status_idx", order = 1) })
	private String receiverId;

	/**
	 * Receving Conditions for Receiver
	 */
	@QuerySqlField(name = "rec_props", notNull = true)
	private Map<String, Object> recProps;

	/**
	 * Sender ID
	 */
	@QuerySqlField(index = true, name = "sender_id", notNull = true)
	private String senderId;

	/**
	 * Delivering Properties for Sender
	 */
	@QuerySqlField(name = "send_props", notNull = true)
	private Map<String, Object> sendProps;

	/**
	 * Message Topic
	 */
	@QuerySqlField(index = true, name = "topic", notNull = true)
	private String topic;

	/**
	 * Payload
	 */
	@QuerySqlField
	private String payload;

	/**
	 * Deliver Source
	 * <ul>
	 * <li>1-From Client's Real-time Message</li>
	 * <li>2-From Client's Last Will</li>
	 * <li>3-From Topic's Retain Message</li>
	 * </ul>
	 */
	@QuerySqlField(name = "delivery_source", notNull = true)
	private Integer DeliverySource;

	/**
	 * Deliver Time
	 */
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@QuerySqlField(index = true, name = "deliver_time", notNull = true)
	private Date deliverTime;

	/**
	 * Expire Time
	 */
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@QuerySqlField(name = "expire_time")
	private Date expireTime;

	/**
	 * Status. 0-initial state；1-sent to consumer already；2-consumer acked
	 */
	@QuerySqlField(notNull = true, orderedGroups = { @QuerySqlField.Group(name = "rec_status_idx", order = 2) })
	private Integer status;

	/**
	 * Acknowledge Time
	 */
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@QuerySqlField(name = "ack_time")
	private Date ackTime;

}
