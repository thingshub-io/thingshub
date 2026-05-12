package io.thingshub.service.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import io.thingshub.transport.DeliverySource;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * <p>
 * Message Delivery
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Accessors(fluent = true)
@Getter
@Builder
@EqualsAndHashCode(callSuper = false)
public class Delivery implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long id;

	private String receiverId;

	private Map<String, Object> recProps;

	private String senderId;

	private Map<String, Object> sendProps;

	private String topic;

	private String payload;

	private DeliverySource deliverySource;

	private Date deliverTime;

	private Date expireTime;

}
