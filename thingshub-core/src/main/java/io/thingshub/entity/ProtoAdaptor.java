package io.thingshub.entity;

import java.io.Serializable;
import java.util.Date;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.commons.LongIdWriter;
import lombok.Data;

/**
 * <p>
 * Protocol Adaptor
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class ProtoAdaptor implements Serializable {

	private static final long serialVersionUID = 6431036327985940723L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * 标识符
	 */
	@QuerySqlField(index = true, name = "identifier", notNull = true, orderedGroups = { @QuerySqlField.Group(name = "identifier_version_idx", order = 1) })
	private String identifier;

	/**
	 * 协议版本
	 */
	@QuerySqlField(index = true, name = "version", notNull = true, orderedGroups = { @QuerySqlField.Group(name = "identifier_version_idx", order = 2) })
	private String version;

	/**
	 * 显示名称
	 */
	@QuerySqlField(name = "title", notNull = true)
	private String title;

	/**
	 * 脚本ID
	 */
	@QuerySqlField(index = true, name = "script_id", notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long scriptId;

	/**
	 * 备注或说明
	 */
	@QuerySqlField
	private String remark;

	/**
	 * 状态。0-正常；1-禁用；
	 */
	@QuerySqlField(notNull = true)
	private Integer status;

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
	@QuerySqlField(name = "create_time", notNull = true)
	private Date createTime;

	/**
	 * 创建者账号名称
	 */
	@QuerySqlField(name = "create_by", notNull = true)
	private String createBy;

	/**
	 * 最后修改时间
	 */
	@JSONField(format = "yyyy-MM-dd HH:mm:ss")
	@QuerySqlField(index = true, name = "update_time")
	private Date updateTime;

	/**
	 * 修改者账号名称
	 */
	@QuerySqlField(name = "update_by")
	private String updateBy;

}