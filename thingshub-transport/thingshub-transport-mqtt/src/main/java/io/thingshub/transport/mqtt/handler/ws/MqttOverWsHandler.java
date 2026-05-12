package io.thingshub.transport.mqtt.handler.ws;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.thingshub.Broker;
import io.thingshub.transport.mqtt.MqttWsServerConfig;
import io.thingshub.transport.mqtt.codec.WebsocketFrameEncoder;
import io.thingshub.transport.mqtt.codec.WebsocketFrameDecoder;
import io.thingshub.transport.mqtt.handler.MQTTPreludeHandler;

public class MqttOverWsHandler extends ChannelInboundHandlerAdapter {

	private final MqttWsServerConfig mqttWsServerConfig;

	public MqttOverWsHandler(MqttWsServerConfig mqttWsServerConfig) {
		this.mqttWsServerConfig = mqttWsServerConfig;
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
			ChannelPipeline pipeline = ctx.pipeline();

			pipeline.addLast(WebsocketFrameDecoder.class.getSimpleName(), Broker.getBean(WebsocketFrameDecoder.class));
			pipeline.addLast(WebsocketFrameEncoder.class.getSimpleName(), Broker.getBean(WebsocketFrameEncoder.class));
			pipeline.addLast(MqttEncoder.class.getName(), MqttEncoder.INSTANCE);
			pipeline.addLast(MqttDecoder.class.getName(), new MqttDecoder(mqttWsServerConfig.getMaxBytesInMessage()));
			pipeline.addLast(MQTTPreludeHandler.class.getSimpleName(), new MQTTPreludeHandler(mqttWsServerConfig));
			ctx.pipeline().remove(this);
		} else {
			super.userEventTriggered(ctx, evt);
		}
	}
}