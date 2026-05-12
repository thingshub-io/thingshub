package io.thingshub.transport;

import java.util.Map;

import io.thingshub.commons.model.ThingshubMessage;
import lombok.Data;

@Data
public class TransportPacket {

	public static TransportPacket EMPTY_PACKET = new TransportPacket() {

		@Override
		public int getPacketId() {
			return 0;
		}

		@Override
		public int getPacketType() {
			return -1;
		}

		@Override
		public String getPacketName() {
			return null;
		}

		@Override
		public long getTimestamp() {
			return System.currentTimeMillis();
		}

		@Override
		public Map<String, Object> getProperties() {
			return null;
		}

		@Override
		public ThingshubMessage getPayload() {
			return null;
		}

	};

	protected int packetId;

	protected int packetType;

	protected String packetName;

	protected long timestamp;

	protected Map<String, Object> properties;

	protected ThingshubMessage payload;

}
