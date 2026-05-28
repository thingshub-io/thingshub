package io.thingshub.transport.mqtt;

import java.util.HashSet;
import java.util.Set;

import io.netty.channel.ChannelHandlerContext;
import io.thingshub.config.TenantSettings;
import io.thingshub.transport.ChannelContextWrapper;

public class MqttChannelContext extends ChannelContextWrapper {

	private final Set<Integer> packetIdsInUsing = new HashSet<>();

	private int receivingCount;

	private boolean requestProblemInfo;

	public MqttChannelContext(ChannelHandlerContext ctx, TenantSettings tenantSettings, String username, String clientId, String clientAddr, int keepalive,
			boolean requestProblemInfo, int protocolVersion, int sessionExpiryInterval) {
		super(ctx, tenantSettings, username, clientId, clientAddr, keepalive, String.valueOf(protocolVersion), sessionExpiryInterval);

		this.requestProblemInfo = requestProblemInfo;
	}

	public boolean isPacketIdUsing(int packetId) {
		return packetIdsInUsing.contains(packetId);
	}

	public void addUsingPacketId(int packetId) {
		packetIdsInUsing.add(packetId);
	}

	public void incReceivingCount() {
		receivingCount++;
	}

	public void removeUsingPacketId(int packetId) {
		packetIdsInUsing.remove(packetId);
		receivingCount = Math.max(receivingCount - 1, 0);
	}

	public void decReceivingCount() {
		receivingCount = Math.max(receivingCount - 1, 0);
	}

	public boolean isRequestProblemInfo() {
		return requestProblemInfo;
	}

}
