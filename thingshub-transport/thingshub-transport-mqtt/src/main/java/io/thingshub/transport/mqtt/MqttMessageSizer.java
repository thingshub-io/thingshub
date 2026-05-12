package io.thingshub.transport.mqtt;

import static io.netty.buffer.ByteBufUtil.utf8Bytes;

import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.thingshub.transport.mqtt.handler.v3.MQTT3MessageSizer;
import io.thingshub.transport.mqtt.handler.v5.MQTT5MessageSizer;

public interface MqttMessageSizer {

	int MIN_CONTROL_PACKET_SIZE = 16;

	static MqttMessageSizer mqtt3() {
		return MQTT3MessageSizer.INSTANCE;
	}

	static MqttMessageSizer mqtt5() {
		return MQTT5MessageSizer.INSTANCE;
	}

	int sizeOf(MqttMessage message);

	default int sizeByHeader(MqttFixedHeader header) {
		int remainingLength = header.remainingLength();
		int fixedHeaderSize = 1;
		int remainingLengthSize = 1; // at least one byte
		if (remainingLength > 127) {
			remainingLengthSize = 2;
			if (remainingLength > 16383) {
				remainingLengthSize = 3;
				if (remainingLength > 2097151) {
					remainingLengthSize = 4;
				}
			}
		}

		return fixedHeaderSize + remainingLengthSize + remainingLength;
	}

	static int sizeBinary(byte[] binary) {
		return 2 + binary.length;
	}

	static int sizeUTF8EncodedString(String s) {
		int rsBytes = utf8Bytes(s);
		// 2 bytes for encoding size prefix in UTF-8 Encoded String, [MQTT5-1.5.7]
		return 2 + rsBytes;
	}

	static int varIntBytes(int i) {
		int bytes = 0;
		do {
			int digit = i % 128;
			i /= 128;
			if (i > 0) {
				digit |= 0x80;
			}
			bytes++;
		} while (i > 0);
		return bytes;
	}
}