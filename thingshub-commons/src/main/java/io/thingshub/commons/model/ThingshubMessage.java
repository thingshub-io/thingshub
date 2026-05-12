package io.thingshub.commons.model;

import java.io.Serializable;

import lombok.Data;
import lombok.ToString;

/**
 * <p>
 * Thingshub Standard Message
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Data
@ToString
public class ThingshubMessage implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id;

	private String clientId;

	private String version;

	private String name;

	private String time;

	/**
	 * 请求参数，JSONObject或JSONArray类型
	 */
	private Object params;

	/**
	 * 回复消息的结果代码
	 */
	private Integer code;

	/**
	 * 回复消息的信息
	 */
	private String message;

	/**
	 * 回复消息返回的数据，JSONObject或JSONArray类型
	 */
	private Object data;

}