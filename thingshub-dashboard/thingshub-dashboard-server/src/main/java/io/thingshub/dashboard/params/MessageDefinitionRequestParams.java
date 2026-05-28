package io.thingshub.dashboard.params;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * <p>
 * 产品消息定义的请求参数
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
public class MessageDefinitionRequestParams {

	@Data
	public static class MessageDefinitionFormParams {

		private Long id;

		/**
		 * 消息名称（消息标识符，由字符、数字或下划线组成）
		 */
		@NotNull(message = "名称不能为空")
		private String name;

		/**
		 * 标题，消息显示名称
		 */
		@NotBlank(message = "标题不能为空")
		private String title;

		/**
		 * 产品编号
		 */
		@NotBlank(message = "产品编号不能为空")
		protected String productCode;

		/**
		 * 消息类型。PROPERTY_POST-属性上报; SERVICE_FUNCTION_CALL-功能调用;
		 * SERVICE_FUNCTION_REPLY-功能调用响应; SERVICE_REQUEST_POST-提交请求;
		 * SERVICE_REQUEST_REPLY-提交请求响应; EVENT_POST-事件上报;
		 */
		@NotNull(message = "消息类型不能为空")
		private String type;

		/**
		 * 物模型标识。消息规范引用物模型中的服务和事件标识符，如果是属性上报消息，值为空
		 */
		private String modelIdentifier;

		/**
		 * 消息topic
		 */
		private String topic;

		/**
		 * 描述
		 */
		private String description;

	}

}
