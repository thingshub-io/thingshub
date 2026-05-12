package io.thingshub.entity;

import java.io.Serializable;
import java.util.Date;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.commons.LongIdWriter;
import lombok.Data;

/**
 * <p>
 * Device Group
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Data
public class DeviceGroup implements Serializable {

	private static final long serialVersionUID = 7288117977001668451L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * Group Name
	 */
	@QuerySqlField(notNull = true)
	private String name;

	/**
	 * 备注或说明
	 */
	@QuerySqlField
	private String remark;

	/**
	 * 组内设备数量
	 */
	@QuerySqlField(name = "device_count", notNull = true)
	private Integer deviceCount = 0;

	/**
	 * 删除状态。0-未删除；1-已删除；
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