package io.thingshub.commons.model;

import java.io.Serializable;

import lombok.Data;

/**
 * <p>
 * Request or Reply Parameter Specification
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
	private DataTypeSpecs dataType;

	/**
	 * 是否必需
	 */
	private boolean required = false;

	/**
	 * 简短描述
	 */
	private String desc;

}