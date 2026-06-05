package io.thingshub.dashboard.params;

import java.util.Date;

import io.thingshub.commons.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 日志的请求参数
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
public class LogRequestParams {

	@Data
	@EqualsAndHashCode(callSuper = true)
	public static class QueryLogParams extends PageParams {

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

	}

}
