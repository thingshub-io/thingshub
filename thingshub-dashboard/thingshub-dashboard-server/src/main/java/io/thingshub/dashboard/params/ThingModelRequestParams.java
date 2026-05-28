package io.thingshub.dashboard.params;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.thingshub.commons.model.DataTypeSpecs;
import lombok.Data;

/**
 * <p>
 * 物模型管理的请求参数
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
public class ThingModelRequestParams {

	@Data
	public static class ThingPropertiesParams {

		/**
		 * 产品编号
		 */
		@NotBlank(message = "产品编号不能为空")
		private String productCode;

		/**
		 * 物模型属性列表
		 */
		private List<ThingPropertyItemParams> properties;

	}

	@Data
	public static class ThingPropertyItemParams {

		/**
		 * 唯一标识符
		 */
		@NotNull(message = "标识符不能为空")
		private String identifier;

		/**
		 * 属性名称
		 */
		@NotNull(message = "名称不能为空")
		private String name;

		/**
		 * 简短描述
		 */
		private String desc;

		/**
		 * 读写模式。r-只读；rw-读写；
		 */
		@NotBlank(message = "读写模式不能为空")
		private String accessMode;

		/**
		 * 数据类型
		 */
		@NotNull(message = "数据类型不能为空")
		private DataTypeSpecs dataType;

	}

	@Data
	public static class ThingServicesParams {

		/**
		 * 产品编号
		 */
		@NotBlank(message = "产品编号不能为空")
		private String productCode;

		/**
		 * 物模型服务列表
		 */
		private List<ThingServiceItemParams> services;

	}

	@Data
	public static class ThingServiceItemParams {

		/**
		 * 服务唯一标识符
		 */
		@NotNull(message = "标识符不能为空")
		private String identifier;

		/**
		 * 服务名称
		 */
		@NotNull(message = "名称不能为空")
		private String name;

		/**
		 * 服务类型。FUNCTION-供外部调用；REQUEST-设备向外部请求；
		 */
		@NotNull(message = "服务类型不能为空")
		private String type;

		/**
		 * 服务的简短描述
		 */
		private String desc;

		/**
		 * 输入参数
		 */
		private List<InOutParameter> inputParameters;

		/**
		 * 输出参数
		 */
		private List<InOutParameter> outputParameters;

	}

	@Data
	public static class ThingEventsParams {

		/**
		 * 产品编号
		 */
		@NotBlank(message = "产品编号不能为空")
		private String productCode;

		/**
		 * 物模型事件列表
		 */
		private List<ThingEventItemParams> events;

	}

	@Data
	public static class ThingEventItemParams {

		/**
		 * 唯一标识符
		 */
		@NotNull(message = "标识符不能为空")
		private String identifier;

		/**
		 * 事件名称
		 */
		@NotNull(message = "名称不能为空")
		private String name;

		/**
		 * 事件的简短描述
		 */
		private String desc;

		/**
		 * 事件参数
		 */
		private List<InOutParameter> parameters;

	}

	@Data
	public static class InOutParameter {

		/**
		 * 参数名称
		 */
		private String name;

		/**
		 * 参数标识符
		 */
		private String identifier;

		/**
		 * 参数数据类型及规范
		 */
		private DataTypeSpecs dataType;

		/**
		 * 参数的简短描述
		 */
		private String desc;
	}

}
