package io.thingshub.dashboard.params;

import javax.validation.constraints.NotBlank;

import lombok.Data;

/**
 * <p>
 * Transport Server管理的请求参数
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
public class TransportServerRequestParams {

	@Data
	public static class PrehandlerParams {

		/**
		 * 服务器名称
		 */
		@NotBlank(message = "服务器名称不能为空")
		private String serverName;

		/**
		 * 脚本语言
		 */
		@NotBlank(message = "脚本语言不能为空")
		private String scriptLang;

		/**
		 * 脚本内容
		 */
		@NotBlank(message = "脚本内容不能为空")
		private String scriptContent;

	}

}
