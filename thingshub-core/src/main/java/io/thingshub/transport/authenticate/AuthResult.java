package io.thingshub.transport.authenticate;

import java.util.List;

import io.netty.handler.codec.mqtt.MqttProperties.UserProperty;
import io.thingshub.service.model.ClientInfo;
import lombok.Getter;
import lombok.experimental.Accessors;

public record AuthResult(ClientInfo clientInfo, ResultCode resultCode, String reasonString, byte[] authData, List<UserProperty> userProperties) {

	public static enum ResultCode {

		OK(0), BAD_PASS(1), NOT_AUTHORIZED(2), BANNED(3), CONTINUE(4), ERROR(9);

		@Accessors(fluent = true)
		@Getter
		private final int value;

		private ResultCode(int val) {
			this.value = val;
		}

		public static ResultCode of(int val) {
			return switch (val) {
			case 0 -> OK;
			case 1 -> BAD_PASS;
			case 2 -> NOT_AUTHORIZED;
			case 3 -> BANNED;
			case 4 -> CONTINUE;
			case 9 -> ERROR;
			default -> null;
			};
		}

	};

	public AuthResult(ClientInfo clientInfo, ResultCode resultCode) {
		this(clientInfo, resultCode, null, null, null);
	}

}
