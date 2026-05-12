package io.thingshub.dashboard.params;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.thingshub.commons.model.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 产品管理的请求参数
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
public class ProductRequestParams {

	@Data
	@EqualsAndHashCode(callSuper = false)
	public static class QueryProductCatParams extends PageParams {

	}

	@Data
	public static class ProductCatFormParams {

		private Long id;

		@NotBlank(message = "品类名称不能为空")
		private String name;

		/**
		 * 说明或备注
		 */
		private String remark;

	}

	@Data
	@EqualsAndHashCode(callSuper = false)
	public static class QueryProductParams extends PageParams {

		private String code;

		private String name;

	}

	@Data
	public static class ProductFormParams {

		private Long id;

		@NotBlank(message = "产品编号不能为空")
		private String code;

		@NotBlank(message = "产品名称不能为空")
		private String name;

		@NotNull(message = "产品品类不能为空")
		private Long catId;

		/**
		 * 产品型号
		 */
		private String model;

		/**
		 * 设备类型。1-直连设备；2-网关设备；3-网关子设备；4-监控设备；
		 */
		@NotNull(message = "设备类型不能为空")
		private Integer type;

		/**
		 * 联网方式。1-通过以太网连接；2-通过蜂窝数据网连接(2G/3G/4G/5G)；3-通过WIFI连接；9-其他；
		 */
		@NotNull(message = "联网方式不能为空")
		private Integer linkMode;

		@NotBlank(message = "协议类型不能为空")
		private String contentType;

		@NotBlank(message = "传输协议不能为空")
		private String transport;

		/**
		 * 产品图片URL列表
		 */
		private List<String> imgs;

		/**
		 * 产品Key
		 */
		private String accessKey;

		/**
		 * 产品秘钥
		 */
		private String accessSecret;

		/**
		 * 备注或说明
		 */
		private String remark;

	}

}
