package io.thingshub.transport;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import com.google.common.collect.Lists;

import cn.hutool.db.sql.Condition;
import io.thingshub.entity.Connection;
import io.thingshub.ioc.Component;
import io.thingshub.service.ConnectionService;
import io.thingshub.service.ConnectionService.ConnKey;
import io.thingshub.service.DeviceService;
import io.thingshub.service.model.ClientType;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

/**
 * <p>
 * Connection Manager
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Component
public class ConnectionManager {

	@Inject
	private ConnectionService connectionService;

	@Inject
	private DeviceService deviceService;

	private final Map<String, BiConsumer<String, String>> kickHandlers = new ConcurrentHashMap<>();

	@PostConstruct
	public void init() {
		connectionService.listen((eventType, connInfo) -> {
			switch (eventType) {
			case CREATED -> {
				if (connInfo.getClientType() == ClientType.DEVICE_CLIENT.value()) {
					deviceService.updateConnectState(connInfo.getClientId(), ConnectState.ON_LINE);
				}
			}
			case REMOVED -> {
				if (connInfo.getClientType() == ClientType.DEVICE_CLIENT.value()) {
					deviceService.updateConnectState(connInfo.getClientId(), ConnectState.OFF_LINE);
				}

				BiConsumer<String, String> consumer = kickHandlers.get(connInfo.getClientId());
				if (consumer != null) {
					consumer.accept(connInfo.getClientId(), connInfo.getChannelId());
				}
			}
			default -> {
			}
			}
		});
	}

	public boolean isOnline(String clientId) {
		return this.getConnectionInfo(clientId) != null;
	}

	public void add(Connection clientConn) {
		ConnKey theKey = new ConnKey(clientConn.getClientId(), clientConn.getChannelId());
		connectionService.save(theKey, clientConn);
	}

	public void remove(String clientId, String channelId) {
		ConnKey theKey = new ConnKey(clientId, channelId);
		connectionService.removeByKey(theKey);
	}

	public Connection getConnectionInfo(String clientId) {
		return connectionService.getByClientId(clientId);
	}

	public void kick(String clientId, BiConsumer<String, String> handler) {
		kickHandlers.putIfAbsent(clientId, handler);
		connectionService.remove(clientId);
	}

	public void cleanConnectionInfoInFailedNode(String nodeId) {
		connectionService.remove(Lists.newArrayList(new Condition("server_node", nodeId)));
	}

}
