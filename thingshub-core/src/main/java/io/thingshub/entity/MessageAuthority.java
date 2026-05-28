package io.thingshub.entity;

import java.io.Serializable;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.commons.LongIdWriter;
import lombok.Data;

/**
 * <p>
 * Message Authority for Service Client
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class MessageAuthority implements Serializable {

	private static final long serialVersionUID = 5296611516819935794L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * Service User Name
	 */
	@QuerySqlField(index = true, name = "service_client", notNull = true)
	private String serviceClient;

	/**
	 * Product Code
	 */
	@QuerySqlField(index = true, name = "product_code", notNull = true)
	private String productCode;

	/**
	 * Message Name
	 */
	@QuerySqlField(index = true, name = "message_name", notNull = true)
	private String messageName;

}