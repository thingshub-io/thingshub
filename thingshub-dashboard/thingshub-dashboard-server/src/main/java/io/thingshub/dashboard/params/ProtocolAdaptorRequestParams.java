package io.thingshub.dashboard.params;

import javax.validation.constraints.NotBlank;

import io.thingshub.commons.model.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 协议适配管理请求参数
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
public class ProtocolAdaptorRequestParams {

	@Data
	@EqualsAndHashCode(callSuper = false)
	public static class QueryProtocolAdaptorParams extends PageParams {

		/**
		 * 协议标识符（产品编号、服务器名称等）
		 */
		@NotBlank(message = "产品编号不能为空")
		private String identifier;

	}

	@Data
	public static class ProtocolAdaptorFormParams {

		/**
		 * ID
		 */
		private Long id;

		/**
		 * 协议标识符
		 */
		@NotBlank(message = "协议不能标识符为空")
		private String identifier;

		/**
		 * 协议名称
		 */
		@NotBlank(message = "协议名称不能为空")
		private String protocolTitle;

		/**
		 * 协议版本
		 */
		@NotBlank(message = "协议版本不能为空")
		private String protocolVersion;

		/**
		 * 脚本ID（更新时不能为空）
		 */
		private Long scriptId;

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

		/**
		 * 备注或说明
		 */
		private String remark;

	}

}
