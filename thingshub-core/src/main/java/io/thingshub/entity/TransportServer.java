package io.thingshub.entity;

import java.io.Serializable;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import com.alibaba.fastjson2.annotation.JSONField;

import io.thingshub.commons.LongIdWriter;
import lombok.Data;

/**
 * <p>
 * Transport Server Info
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
@Data
public class TransportServer implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@QuerySqlField(index = true, notNull = true)
	@JSONField(serializeUsing = LongIdWriter.class)
	private Long id;

	/**
	 * Server Name
	 */
	@QuerySqlField(index = true, name = "server_name")
	private String serverName;

	/**
	 * Transport Protocol. Such as TCP, MQTT, MQTT_WS, HTTP, UDP, GB28181 etc.
	 */
	@QuerySqlField
	private String transport;

}