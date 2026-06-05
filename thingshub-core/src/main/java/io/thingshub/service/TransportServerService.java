package io.thingshub.service;

import java.util.List;

import com.google.common.collect.Lists;

import cn.hutool.db.sql.Condition;
import io.thingshub.commons.ServiceException;
import io.thingshub.entity.ScriptInfo;
import io.thingshub.entity.TransportServer;
import io.thingshub.ioc.Service;
import io.thingshub.script.ScriptEngineFactory;
import io.thingshub.service.base.BaseService;
import io.thingshub.service.base.IdGenerator;
import jakarta.inject.Inject;

/**
 * <p>
 * Transport Server Info Service
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class TransportServerService extends BaseService<Long, TransportServer> {

	@Inject
	private ScriptService scriptService;

	@Inject
	private ScriptEngineFactory scriptEngineFactory;

	@Inject
	private IdGenerator idGenerator;

	public TransportServer getTransportServer(String serverName) {
		return this.getOne(Lists.newArrayList(new Condition("server_name", serverName)));
	}

	public Long saveTransportServer(String serverName, String transport) throws ServiceException {
		List<Condition> qryConditions = Lists.newArrayList(new Condition("server_name", serverName));
		TransportServer transportServer = this.getOne(qryConditions);
		if (transportServer == null) {
			transportServer = new TransportServer();
			transportServer.setId(idGenerator.nextId());
		}

		transportServer.setServerName(serverName);
		transportServer.setTransport(transport);
		this.save(transportServer.getId(), transportServer);

		return transportServer.getId();
	}

	public void setPrehandler(String serverName, String scriptLang, String scriptContent) {
		List<Condition> qryConditions = Lists.newArrayList(new Condition("server_name", serverName));
		TransportServer transportServer = this.getOne(qryConditions);

		if (transportServer != null) {
			ScriptInfo scriptInfo = scriptService.getScript(serverName);
			if (scriptInfo == null) {
				long scriptId = idGenerator.nextId();
				scriptInfo = new ScriptInfo();
				scriptInfo.setId(scriptId);
				scriptInfo.setName(serverName);
			}

			scriptInfo.setLang(scriptLang);
			scriptInfo.setContent(scriptContent);
			scriptInfo.setDeletedStatus(DeletedStatus.NOT_DELETED.value());
			scriptService.save(scriptInfo.getId(), scriptInfo);

			scriptEngineFactory.getScriptEngine(scriptInfo.getLang()).load(scriptInfo.getName(), scriptInfo.getContent());
		}
	}
}
