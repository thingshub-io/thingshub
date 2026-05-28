package io.thingshub.commons.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * <p>
 * 产品消息规范
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class MessageSpec implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	private Long id;

	/**
	 * 消息名称（消息标识符，由字符、数字或下划线组成）
	 */
	private String name;

	/**
	 * 消息标题
	 */
	private String title;

	/**
	 * 产品编号
	 */
	private String productCode;

	/**
	 * 消息类型
	 * <ul>
	 * <li>PROPERTY_POST - 属性上报</li>
	 * <li>PROPERTY_REPLY - 属性上报回复</li>
	 * <li>SERVICE_FUNCTION_CALL - 功能调用</li>
	 * <li>SERVICE_FUNCTION_REPLY - 功能调用回复</li>
	 * <li>SERVICE_REQUEST_POST - 提交请求</li>
	 * <li>SERVICE_REQUEST_REPLY - 请求回复</li>
	 * <li>EVENT_POST - 事件上报</li>
	 * <li>EVENT_REPLY - 事件回复</li>
	 * </ul>
	 */
	private String type;

	/**
	 * 消息参数
	 */
	private List<MessageParameter> parameters;

	/**
	 * 消息topic
	 */
	private String topic;

	/**
	 * 描述
	 */
	private String description;

}