package io.thingshub.transport.gb28181.subscribe;

public enum EventType {
	ACK, TIMEOUT, RESPONSE, TRANSACTIONTERMINATED, DIALOGTERMINATED,
	// 设备未找到
	deviceNotFoundEvent,
	// 设备未找到
	cmdSendFailEvent,
}