package io.thingshub.commons.model;

import java.io.Serializable;
import java.util.Map;

import lombok.Data;

/**
 * <p>
 * Data Type Specification
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class DataTypeSpecs implements Serializable {

	private static final long serialVersionUID = -8340400368305760888L;

	/**
	 * 类型:
	 * int（原生）、float（原生）、double（原生）、text（原生）、date（String类型UTC毫秒）、bool（0或1的int类型）、enum（int类型，枚举项定义方法与bool类型定义0和1的值方法相同）
	 */
	private String type;

	/**
	 * 数据取值规范
	 */
	private Map<String, Object> specs;

}