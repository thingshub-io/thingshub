package io.thingshub.commons;

/**
 * <p>
 * Thingshub Constants
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
public abstract class ThingshubConstants {

	public static final String THINGSHUB_CLIENT_ID_PLACEHOLDER = "${thingshub.client.id}";

	public static final String TOPIC_PREFIX = "sys";

	public static final String SERVICE_CLIENT_ALL = "all";

	public static final String THING_TOPIC_PROPERTY_POST = TOPIC_PREFIX + "/thing/%s/%s/property/post/%s";
	public static final String THING_TOPIC_PROPERTY_REPLY = TOPIC_PREFIX + "/thing/%s/%s/property/reply/%s";
	public static final String THING_TOPIC_SERVICE_FUNCTION_CALL = TOPIC_PREFIX + "/thing/%s/%s/service/function_call/%s";
	public static final String THING_TOPIC_SERVICE_FUNCTION_REPLY = TOPIC_PREFIX + "/thing/%s/%s/service/function_reply/%s";
	public static final String THING_TOPIC_SERVICE_REQUEST_POST = TOPIC_PREFIX + "/thing/%s/%s/service/request_post/%s";
	public static final String THING_TOPIC_SERVICE_REQUEST_REPLY = TOPIC_PREFIX + "/thing/%s/%s/service/request_reply/%s";
	public static final String THING_TOPIC_EVENT_POST = TOPIC_PREFIX + "/thing/%s/%s/event/post/%s";
	public static final String THING_TOPIC_EVENT_REPLY = TOPIC_PREFIX + "/thing/%s/%s/event/reply/%s";

	public static final String SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_PRODUCT_BINDING = "query_product_binding";
	public static final String SERVICE_CLIENT_INTERNAL_TOPIC_QUERY_PRODUCT_BINDING = //
			TOPIC_PREFIX + "/service_client/internal/%s/%s/service/request_post/" + SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_PRODUCT_BINDING;

	public static final String SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_PRODUCT_BINDING_ACK = "query_product_binding_ack";
	public static final String SERVICE_CLIENT_INTERNAL_TOPIC_QUERY_PRODUCT_BINDING_REPLY = //
			TOPIC_PREFIX + "/service_client/internal/%s/%s/service/request_reply/" + SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_PRODUCT_BINDING_ACK;

	public static final String SERVICE_CLIENT_INTERNAL_MESSAGE_CHANGE_PRODUCT_BINDING = "change_product_binding";
	public static final String SERVICE_CLIENT_INTERNAL_TOPIC_CHANGE_PRODUCT_BINDING = //
			TOPIC_PREFIX + "/service_client/internal/%s/%s/service/function_call/" + SERVICE_CLIENT_INTERNAL_MESSAGE_CHANGE_PRODUCT_BINDING;

	public static final String SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_MESSAGE_DEFINITION = "query_message_definition";
	public static final String SERVICE_CLIENT_INTERNAL_TOPIC_QUERY_MESSAGE_DEFINITION = //
			TOPIC_PREFIX + "/service_client/internal/%s/%s/service/request_post/" + SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_MESSAGE_DEFINITION;

	public static final String SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_MESSAGE_DEFINITION_ACK = "message_definition_query_ack";
	public static final String SERVICE_CLIENT_INTERNAL_TOPIC_QUERY_MESSAGE_DEFINITION_REPLY = //
			TOPIC_PREFIX + "/service_client/internal/%s/%s/service/request_reply/" + SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_MESSAGE_DEFINITION_ACK;

	public static final String SERVICE_CLIENT_INTERNAL_MESSAGE_CHANGE_MESSAGE_AUTHORIZATION = "change_message_authorization";
	public static final String SERVICE_CLIENT_INTERNAL_TOPIC_CHANGE_MESSAGE_AUTHORIZATION = //
			TOPIC_PREFIX + "/service_client/internal/%s/%s/service/function_call/" + SERVICE_CLIENT_INTERNAL_MESSAGE_CHANGE_MESSAGE_AUTHORIZATION;

	public static final String SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_DEVICE = "query_device";
	public static final String SERVICE_CLIENT_INTERNAL_TOPIC_QUERY_DEVICE = //
			TOPIC_PREFIX + "/service_client/internal/%s/%s/service/request_post/" + SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_DEVICE;

	public static final String SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_DEVICE_ACK = "query_device_ack";
	public static final String SERVICE_CLIENT_INTERNAL_TOPIC_QUERY_DEVICE_REPLY = //
			TOPIC_PREFIX + "/service_client/internal/%s/%s/service/request_reply/" + SERVICE_CLIENT_INTERNAL_MESSAGE_QUERY_DEVICE_ACK;

	public static final String SERVICE_CLIENT_INTERNAL_MESSAGE_DEVICE_INFO = "device_info";
	public static final String SERVICE_CLIENT_INTERNAL_TOPIC_DEVICE_INFO = //
			TOPIC_PREFIX + "/service_client/internal/%s/%s/service/request_post/" + SERVICE_CLIENT_INTERNAL_MESSAGE_DEVICE_INFO;

	public static final String SERVICE_CLIENT_INTERNAL_MESSAGE_DEVICE_INFO_ACK = "device_info_ack";
	public static final String SERVICE_CLIENT_INTERNAL_TOPIC_DEVICE_INFO_REPLY = //
			TOPIC_PREFIX + "/service_client/internal/%s/%s/service/request_reply/" + SERVICE_CLIENT_INTERNAL_MESSAGE_DEVICE_INFO_ACK;

}