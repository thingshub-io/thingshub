package io.thingshub.service.base;

public class QueryFilter {

	/**
	 * 数据库字段名称
	 */
	private String field;

	/**
	 * 运算符（大于号，小于号，等于号 like 等）
	 */
	private String operator;

	/**
	 * 值
	 */
	private Object value;

}
