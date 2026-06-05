package io.thingshub.commons;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>
 * Message Parameter
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class MessageParameter implements Serializable {

	private static final long serialVersionUID = -1904714138551473008L;

	/**
	 * 名称
	 */
	private String name;

	/**
	 * 标识符
	 */
	private String identifier;

	/**
	 * 数据类型及规范
	 */
	private DataTypeSpecs dataTypeSpecs;

	/**
	 * 是否必需
	 */
	private boolean required = false;

	/**
	 * 消息参数的简短描述
	 */
	private String desc;

}