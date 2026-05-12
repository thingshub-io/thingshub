package io.thingshub.service.model;

import java.io.Serializable;
import java.util.Date;

import com.alibaba.fastjson2.annotation.JSONField;

import lombok.Data;

/**
 * <p>
 * 协议适配器详细信息
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class ProtocolAdaptorDetails implements Serializable {

	private static final long serialVersionUID = 6431036327985940723L;

	/**
	 * ID
	 */
	private Long id;

	/**
	 * 协议标识符
	 */
	private String identifier;

	/**
	 * 协议版本
	 */
	private String version;

	/**
	 * 协议名称
	 */
	private String title;

	/**
	 * 脚本ID
	 */
	private Long scriptId;

	/**
	 * 脚本语言
	 */
	private String scriptLang;

	/**
	 * 脚本内容
	 */
	private String scriptContent;

	/**
	 * 备注或说明
	 */
	private String remark;

	/**
	 * 状态。0-正常；1-禁用；
	 */
	private Integer status;

	/**
	 * 创建时间
	 */
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

	/**
	 * 创建者账号名称
	 */
	private String createBy;

	/**
	 * 最后修改时间
	 */
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	private Date updateTime;

	/**
	 * 修改者账号名称
	 */
	private String updateBy;

}