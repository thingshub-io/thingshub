package io.thingshub.logging;

import java.util.Date;

import lombok.Data;

@Data
public class LogSearchParams {

	/**
	 * 日志级别
	 */
	private String level;

	/**
	 * 开始时间
	 */
	private Date start;

	/**
	 * 截止时间
	 */
	private Date end;

	/**
	 * 关键字
	 */
	private String keyword;

	/**
	 * 当前页码，默认为1
	 */
	private Integer page = 1;

	/**
	 * 每页记录数，默认为10
	 */
	private Integer size = 10;

}