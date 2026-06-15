package io.thingshub.transport;

import java.io.Serializable;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * Publishing Record
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class Publication implements Serializable {

	private static final long serialVersionUID = 7433413039128565398L;

	/**
	 * ID
	 */
	private Long id;

	/**
	 * Publisher User Name
	 */
	private String username;

	/**
	 * Publisher Client ID
	 */
	private String clientId;

	/**
	 * Publish Way
	 * <ul>
	 * <li>1-Client Publish Directly</li>
	 * <li>2-Client Publish by Last Will</li>
	 * </ul>
	 */
	private Integer pubWay;

	/**
	 * Publish Topic
	 */
	private String topic;

	/**
	 * To Thingshub's Standard Topic
	 */
	private String stdTopic;

	/**
	 * Message Properties
	 */
	private Map<String, Object> props;

	/**
	 * Standard Payload
	 */
	private String stdPayload;

	/**
	 * Payload
	 */
	private String payload;

}
