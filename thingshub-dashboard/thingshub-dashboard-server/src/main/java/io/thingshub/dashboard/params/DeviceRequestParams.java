package io.thingshub.dashboard.params;

import java.math.BigDecimal;

import javax.validation.constraints.NotBlank;

import io.thingshub.commons.model.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 设备管理的请求参数
 * </p>
 *
 * @author Albert
 * @since 1.0.0
 */
public class DeviceRequestParams {

	@Data
	@EqualsAndHashCode(callSuper = false)
	public static class QueryDeviceGroupParams extends PageParams {

	}

	@Data
	public static class DeviceGroupFormParams {

		private Long id;

		@NotBlank(message = "分组名称不能为空")
		private String name;

		/**
		 * 备注或说明
		 */
		private String remark;

	}

	@Data
	@EqualsAndHashCode(callSuper = false)
	public static class QueryDeviceParams extends PageParams {

		private String sn;

	}

	@Data
	public static class PublishParams {

		private String sn;

	}

	@Data
	public static class DeviceFormParams {

		private Long id;

		/**
		 * 设备编号
		 */
		@NotBlank(message = "设备编号不能为空")
		private String sn;

		/**
		 * 设备登录账号名称
		 */
		private String username;

		/**
		 * 设备登录账号密码
		 */
		private String password;

		/**
		 * 所在地区行政编码
		 */
		private String area;

		/**
		 * 详细地址
		 */
		private String address;

		/**
		 * 纬度
		 */
		private BigDecimal lat;

		/**
		 * 经度
		 */
		private BigDecimal lng;

		/**
		 * 二维码
		 */
		private String qrCode;

		/**
		 * 分组
		 */
		private Long groupId;

		/**
		 * 设备所属产品
		 */
		@NotBlank(message = "产品不能为空")
		private String productCode;

		/**
		 * 备注或说明
		 */
		private String remark;

	}

	@Data
	public static class DeviceSnParams {

		private String sn;

	}

}
