package io.thingshub.service;

import com.google.common.collect.Lists;

import cn.hutool.db.sql.Condition;
import io.thingshub.entity.Connection;
import io.thingshub.ioc.Service;
import io.thingshub.service.ConnectionService.ConnKey;
import io.thingshub.service.base.BaseService;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * Connection State Service
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class ConnectionService extends BaseService<ConnKey, Connection> {

	@AllArgsConstructor
	@Getter
	public static class ConnKey {

		private String clientId;

		private String channelId;

	};

	protected int getBackups() {
		return 0;
	}

	public Connection getByClientId(String clientId) {
		return this.getOne(Lists.newArrayList(new Condition("client_id", clientId)));
	}

	public void remove(String clientId) {
		this.remove(Lists.newArrayList(new Condition("client_id", clientId)));
	}

}
