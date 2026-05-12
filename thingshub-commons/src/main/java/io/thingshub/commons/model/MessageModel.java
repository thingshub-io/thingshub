package io.thingshub.commons.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * <p>
 * 产品消息模型
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class MessageModel implements Serializable {

	private static final long serialVersionUID = 7720699406836904572L;

	/**
	 * ID
	 */
	private Long id;

	/**
	 * 消息名称（消息标识符，由字符、数字或下划线组成）
	 */
	private String name;

	/**
	 * 消息显示名称
	 */
	private String title;

	/**
	 * 产品编号
	 */
	private String productCode;

	/**
	 * 消息类别。PROPERTY-属性上报消息；SERVICE-服务调用消息；EVENT-事件消息；
	 */
	private String cat;

	/**
	 * 消息参数定义
	 */
	private List<MessageParameter> parameters;

	/**
	 * 消息类型。REQUEST，REPLY
	 */
	private String type;

	/**
	 * 消息topic
	 */
	private String topic;

	/**
	 * 描述
	 */
	private String description;

}