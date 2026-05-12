package io.thingshub.entity;

import java.io.Serializable;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.commons.LongIdWriter;
import lombok.Data;

/**
 * <p>
 * Script Info
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class ScriptInfo implements Serializable {

	private static final long serialVersionUID = 1495703771784177384L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * Name
	 */
	@QuerySqlField(index = true, notNull = true)
	private String name;

	/**
	 * Script Language. Such as javascript, python, etc.
	 */
	@QuerySqlField(index = true, notNull = true)
	private String lang;

	/**
	 * Script Code
	 */
	@QuerySqlField(notNull = true)
	private String content;

	/**
	 * Delete Status。0-Undeleted；1-Deleted；
	 */
	@JSONField(serialize = false)
	@QuerySqlField(name = "deleted_status", notNull = true)
	private Integer deletedStatus;

	/**
	 * Remark
	 */
	@QuerySqlField
	private String remark;

}