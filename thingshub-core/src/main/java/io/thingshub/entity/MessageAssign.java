package io.thingshub.entity;

import java.io.Serializable;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.commons.LongIdWriter;
import lombok.Data;

/**
 * <p>
 * Message Permission Assignment for Client User
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class MessageAssign implements Serializable {

	private static final long serialVersionUID = 5296611516819935794L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * Client User Name
	 */
	@QuerySqlField(index = true, name = "client_user_name", notNull = true)
	private String clientUserName;

	/**
	 * Product Code
	 */
	@QuerySqlField(index = true, name = "product_code", notNull = true)
	private String productCode;

	/**
	 * Message Specification ID
	 */
	@QuerySqlField(index = true, name = "msg_spec_id", notNull = true)
	private Long msgSpecId;

}