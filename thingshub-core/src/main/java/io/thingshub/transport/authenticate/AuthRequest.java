package io.thingshub.transport.authenticate;

import java.util.List;

import io.netty.handler.codec.mqtt.MqttProperties.UserProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
@Builder
@ToString
public class AuthRequest {

	private String clientId;

	private String username;

	private String password;

	private byte[] cert;

	private String remoteAddr;

	private int remotePort;

	private boolean requestResponseInfomation;

	private List<UserProperty> userProperties;

	private String authenticationMethod;

	private byte[] authenticationData;

	private boolean isReAuth;

}
