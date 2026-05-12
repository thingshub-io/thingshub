package io.thingshub.commons.model;

/**
 * <p>
 * Thingshub Constants
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
public abstract class ThingshubConstants {

	public static final String THING_TOPIC_PREFIX = "sys";

	public static final String THING_PROPERTY_POST_REPLY_TOPIC_FORMAT = THING_TOPIC_PREFIX + "/thing/%s/%s/property/post/reply/%s";
	public static final String THING_SERVICE_CALL_TOPIC_FORMAT = THING_TOPIC_PREFIX + "/thing/%s/%s/service/call/%s";
	public static final String THING_EVENT_POST_REPLY_TOPIC_FORMAT = THING_TOPIC_PREFIX + "/thing/%s/%s/event/post/reply/%s";

	public static final String THING_PROPERTY_POST_TOPIC_FORMAT = THING_TOPIC_PREFIX + "/thing/%s/%s/property/post/%s";
	public static final String THING_SERVICE_CALL_REPLY_TOPIC_FORMAT = THING_TOPIC_PREFIX + "/thing/%s/%s/service/call/reply/%s";
	public static final String THING_EVENT_POST_TOPIC_FORMAT = THING_TOPIC_PREFIX + "/thing/%s/%s/event/post/%s";

	public static final String INTERNAL_MESSAGE_QUERY_BOUND_PRODUCT = "query_bound_product";
	public static final String INTERNAL_TOPIC_BOUND_PRODUCT_QUERY = THING_TOPIC_PREFIX + "/internal/bound_product/query";
	public static final String INTERNAL_MESSAGE_QUERY_BOUND_PRODUCT_ACK = "query_bound_product_ack";
	public static final String INTERNAL_TOPIC_BOUND_PRODUCT_QUERY_REPLY = THING_TOPIC_PREFIX + "/internal/client-user/%s/%s/bound_product/query/reply";

	public static final String INTERNAL_MESSAGE_BOUND_PRODUCT_CHANGED = "bound_product_changed";
	public static final String INTERNAL_TOPIC_BOUND_PRODUCT_CHANGED = THING_TOPIC_PREFIX + "/internal/client-user/%s/bound_product/changed";

	public static final String INTERNAL_MESSAGE_QUERY_MESSAGE_MODEL = "query_message_model";
	public static final String INTERNAL_TOPIC_MESSAGE_MODEL_QUERY = THING_TOPIC_PREFIX + "/internal/message_model/query";
	public static final String INTERNAL_MESSAGE_QUERY_MESSAGE_MODEL_ACK = "query_message_model_ack";
	public static final String INTERNAL_TOPIC_MESSAGE_MODEL_QUERY_REPLY = THING_TOPIC_PREFIX + "/internal/client-user/%s/%s/message_model/query/reply";

	public static final String INTERNAL_MESSAGE_MESSAGE_MODEL_CHANGED = "message_model_changed";
	public static final String INTERNAL_TOPIC_MESSAGE_MODEL_CHANGED = THING_TOPIC_PREFIX + "/internal/product/%s/message_model/changed";

	public static final String INTERNAL_MESSAGE_QUERY_DEVICE = "query_device";
	public static final String INTERNAL_TOPIC_DEVICE_QUERY = THING_TOPIC_PREFIX + "/internal/device/query";
	public static final String INTERNAL_MESSAGE_QUERY_DEVICE_ACK = "query_device_ack";
	public static final String INTERNAL_TOPIC_DEVICE_QUERY_REPLY = THING_TOPIC_PREFIX + "/internal/client-user/%s/%s/device/query/reply";

	public static final String INTERNAL_MESSAGE_DEVICE_INFO = "device_info";
	public static final String INTERNAL_TOPIC_DEVICE_INFO = THING_TOPIC_PREFIX + "/internal/device/info";
	public static final String INTERNAL_MESSAGE_DEVICE_INFO_ACK = "device_info_ack";
	public static final String INTERNAL_TOPIC_DEVICE_INFO_REPLY = THING_TOPIC_PREFIX + "/internal/client-user/%s/%s/device/info/reply";

	public static final String INTERNAL_MESSAGE_DISABLE_DEVICE = "disable_device";
	public static final String INTERNAL_TOPIC_DEVICE_DISABLE = THING_TOPIC_PREFIX + "/internal/device/disable";
	public static final String INTERNAL_MESSAGE_DISABLE_DEVICE_ACK = "disable_device_ack";
	public static final String INTERNAL_TOPIC_DEVICE_DISABLE_REPLY = THING_TOPIC_PREFIX + "/internal/client-user/%s/%s/device/disable/reply";

	public static final String INTERNAL_MESSAGE_ENABLE_DEVICE = "enable_device";
	public static final String INTERNAL_TOPIC_DEVICE_ENABLE = THING_TOPIC_PREFIX + "/internal/device/enable";
	public static final String INTERNAL_MESSAGE_ENABLE_DEVICE_ACK = "enable_device_ack";
	public static final String INTERNAL_TOPIC_DEVICE_ENABLE_REPLY = THING_TOPIC_PREFIX + "/internal/client-user/%s/%s/device/enable/reply";

}