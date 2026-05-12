package io.thingshub.entity;

import java.util.Date;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.commons.LongIdWriter;
import io.thingshub.service.base.DataRegion;
import lombok.Data;

/**
 * <p>
 * Selection in Group
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Data
@DataRegion(name = "transient_region", persistent = false, local = false)
public class GroupSelection {

	/**
	 * Data ID
	 */
	@QuerySqlField(index = true, name = "data_id", notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private String dataId;

	/**
	 * Group Name
	 */
	@QuerySqlField(index = true, notNull = true)
	private String group;

	/**
	 * The Selected One in Group Candidates
	 */
	@QuerySqlField(notNull = true)
	private String winner;

	/**
	 * Create Time
	 */
	@QuerySqlField(name = "create_time", notNull = true)
	private Date createTime;

}