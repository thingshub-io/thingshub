package io.thingshub.dashboard.params;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.thingshub.commons.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 系统用户管理接口请求参数
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
public class ServiceClientRequestParams {

	@Data
	@EqualsAndHashCode(callSuper = false)
	public static class QueryServiceClientParams extends PageParams {

	}

	@Data
	public static class ServiceClientFormParams {

		private Long id;

		/**
		 * 客户端用户名
		 */
		@NotBlank(message = "用户名不能为空")
		protected String username;

		/**
		 * 客户端密码
		 */
		@NotBlank(message = "密码不能为空")
		private String password;

		/**
		 * 业务系统名称
		 */
		@NotNull(message = "业务系统名称不能为空")
		private String serviceName;

		/**
		 * 备注说明
		 */
		private String remark;

	}

	@Data
	public static class BindProductParams {

		/**
		 * 业务系统客户端ID
		 */
		@NotNull(message = "业务系统客户端ID不能为空")
		private Long id;

		/**
		 * 被关联的产品编号
		 */
		@NotNull(message = "产品编号不能为空")
		private List<String> productCodes;

	}

	@Data
	public static class MessageAuthorityParams {

		/**
		 * 客户端用户名
		 */
		@NotBlank(message = "用户名不能为空")
		protected String username;

		/**
		 * 产品编码
		 */
		@NotBlank(message = "产品编码不能为空")
		private String productCode;

		/**
		 * 消息名称列表。空列表表示取消产品消息的所有授权
		 */
		@NotNull(message = "消息名称不能为空")
		private List<String> messages;

	}

}
