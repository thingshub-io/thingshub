package io.thingshub.entity;

import java.io.Serializable;
import java.util.Date;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.commons.LongIdWriter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 设备告警规则配置
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class AlarmProfile implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 告警规则ID
	 */
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * 产品名称
	 */
	private String productName;

	/**
	 * 告警名称
	 */
	private String name;

	/**
	 * 告警级别。1-提醒通知；2-轻微问题；3-严重警报；
	 */
	private Integer severity;

	/**
	 * 产品编号
	 */
	private String productCode;

	/**
	 * 触发器
	 */
	private String triggers;

	/**
	 * 执行动作
	 */
	private String actions;

	/**
	 * 备注或说明
	 */
	private String remark;

	/**
	 * 状态。0-正常；1-禁用；
	 */
	@QuerySqlField(notNull = true)
	private Integer status;

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