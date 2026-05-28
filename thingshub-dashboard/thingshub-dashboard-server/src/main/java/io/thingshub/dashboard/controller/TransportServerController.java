package io.thingshub.dashboard.controller;

import java.util.List;

import io.thingshub.dashboard.params.TransportServerRequestParams.PrehandleScriptParams;
import io.thingshub.entity.TransportServer;
import io.thingshub.http.HttpMethod;
import io.thingshub.http.annotation.Controller;
import io.thingshub.http.annotation.RequestBody;
import io.thingshub.http.annotation.RequestMapping;
import io.thingshub.service.TransportServerService;
import jakarta.inject.Inject;

/**
 * <p>
 * Transport Server Management API
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Controller
public class TransportServerController {

	@Inject
	private TransportServerService transportServerService;

	@RequestMapping(path = "/transport-server/list")
	public List<TransportServer> listTransportServers() {
		return transportServerService.list();
	}

	@RequestMapping(method = HttpMethod.POST, path = "/transport-server/prehandle-script")
	public void setPrehandleScript(@RequestBody PrehandleScriptParams params) {
		transportServerService.setPrehandleScript(params.getServerName(), params.getScriptLang(), params.getScriptContent());
	}

}