package io.thingshub.dashboard.params;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * <p>
 * 产品消息规范管理的请求参数
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
public class MessageSpecRequestParams {

	@Data
	public static class MessageSpecFormParams {

		private Long id;

		/**
		 * 产品编号
		 */
		@NotBlank(message = "产品编号不能为空")
		protected String productCode;

		/**
		 * 标题，消息显示名称
		 */
		@NotBlank(message = "标题不能为空")
		private String title;

		/**
		 * 消息名称（消息标识符，由字符、数字或下划线组成）
		 */
		@NotNull(message = "名称不能为空")
		private String name;

		/**
		 * 消息类别。PROPERTY-属性读写消息；SERVICE-服务调用消息；EVENT-事件消息；
		 */
		@NotBlank(message = "消息类别不能为空")
		private String cat;

		/**
		 * 荷载参数定义。通过关联的物模型ID确定消息的参数数量、参数名称、参数类型及参数取值规范
		 */
//		@NotEmpty(message = "参数ID不能为空")
		private List<ParameterSpec> parameterSpecs;

		/**
		 * 类型。REQUEST-请求类型的消息；REPLY-回复类型消息；
		 */
		@NotNull(message = "消息类型不能为空")
		private String type;

		/**
		 * 消息topic。对于发布者，用于消息发布，对于订阅者，用于消息订阅
		 */
		private String topic;

		/**
		 * 描述
		 */
		private String description;

	}

	@Data
	public static class ParameterSpec {

		/**
		 * 物模型标识符。参数的类型信息在物模型中定义
		 */
		private String refId;

		/**
		 * 参数是否必需
		 */
		private boolean required;
	}

}
