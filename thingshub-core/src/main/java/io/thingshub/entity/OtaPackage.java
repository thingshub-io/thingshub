package io.thingshub.entity;

import java.io.Serializable;
import java.util.Date;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.commons.LongIdWriter;
import lombok.Data;

/**
 * <p>
 * 升级包信息
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class OtaPackage implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * 升级包名称
	 */
	@QuerySqlField(notNull = true)
	private String name;

	/**
	 * 升级包类型。1-软件包；2-固件包；
	 */
	@QuerySqlField(notNull = true)
	private Integer type;

	/**
	 * 产品ID
	 */
	@QuerySqlField(index = true, name = "product_id", notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long productId;

	/**
	 * 产品编号
	 */
	@QuerySqlField(index = true, name = "product_code", notNull = true)
	private String productCode;

	/**
	 * 产品名称
	 */
	@QuerySqlField(name = "product_name", notNull = true)
	private String productName;

	/**
	 * 版本
	 */
	@QuerySqlField(notNull = true)
	private String version;

	/**
	 * 升级包地址
	 */
	@QuerySqlField(name = "package_url", notNull = true)
	private String packageUrl;

	/**
	 * 升级包大小。单位：KB
	 */
	@QuerySqlField(name = "package_size", notNull = true)
	private Integer packageSize;

	/**
	 * 备注或说明
	 */
	@QuerySqlField
	private String remark;

	/**
	 * 校验和
	 */
	@QuerySqlField
	private String checksum;

	/**
	 * 校验和算法
	 */
	@QuerySqlField(name = "checksum_alg")
	private String checksumAlg;

	/**
	 * 删除状态。0-未删除；1-已删除
	 */
	@JSONField(serialize = false)
	@QuerySqlField(name = "deleted_status", notNull = true)
	private Integer deletedStatus;

	/**
	 * 创建时间
	 */
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@QuerySqlField(index = true, name = "create_time", notNull = true)
	private Date createTime;

	/**
	 * 创建者账号名称
	 */
	@QuerySqlField(name = "create_by", notNull = true)
	private String createBy;

}