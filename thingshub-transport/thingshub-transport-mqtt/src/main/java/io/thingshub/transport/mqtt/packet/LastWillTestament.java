package io.thingshub.transport.mqtt.packet;

import java.io.Serializable;
import java.util.List;

import io.netty.handler.codec.mqtt.MqttProperties.StringPair;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
@Builder
public class LastWillTestament implements Serializable {

	private static final long serialVersionUID = 1L;

	private boolean retain;

	private String topic;

	private Integer qos;

	private byte[] payload;

	// MQTT 5
	private int expiryInterval;

	private Boolean payloadFormatIndicator;

	private String contentType;

	private String responseTopic;

	private byte[] correlationData;

	private List<StringPair> userProperties;

	private int delayInterval;

}
