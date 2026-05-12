package io.thingshub.transport.mqtt.handler;

import io.netty.handler.codec.mqtt.MqttMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ValidateMessageResult {

	public enum Action {
		CONTINUE, RESPONSE, FAREWELL, JUST_CLOSE
	}

	private Action action;

	private MqttMessage response;

}