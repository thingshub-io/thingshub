package io.thingshub.transport.mqtt.handler;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.thingshub.service.model.ClientInfo;

public record MQTTAuthResult(ClientInfo clientInfo, boolean isFarewell, MqttMessage reply, String reason) {

}
