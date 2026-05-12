package io.thingshub.entity;

import java.io.Serializable;
import java.util.Date;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.commons.LongIdWriter;
import lombok.Data;

/**
 * <p>
 * Message Specification
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class MessageSpec implements Serializable {

	private static final long serialVersionUID = 7720699406836904572L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * 产品编号
	 */
	@QuerySqlField(index = true, name = "product_code", notNull = true)
	private String productCode;

	/**
	 * 消息显示名称
	 */
	@QuerySqlField(notNull = true)
	private String title;

	/**
	 * 消息名称（消息标识符，由字符、数字或下划线组成）
	 */
	@QuerySqlField(index = true, notNull = true)
	private String name;

	/**
	 * 消息类别。PROPERTY-属性上报消息；SERVICE-服务调用消息；EVENT-事件消息；
	 */
	@QuerySqlField(notNull = true)
	private String cat;

	/**
	 * 荷载参数定义。通过关联的物模型标识符来确定消息的参数数量、名称及类型信息
	 */
	@QuerySqlField(name = "parameter_specs")
	private String parameterSpecs;

	/**
	 * 消息类型。REQUEST，REPLY
	 */
	@QuerySqlField(notNull = true)
	private String type;

	/**
	 * 消息topic
	 */
	@QuerySqlField(notNull = true)
	private String topic;

	/**
	 * 描述
	 */
	@QuerySqlField
	private String description;

	/**
	 * 删除状态，默认为0。0-未删除；1-已删除；
	 */
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
	@QuerySqlField(notNull = true)
	private String createBy;

}