package io.thingshub.transport.http.interceptor;

import io.thingshub.Broker;
import io.thingshub.commons.model.ThingshubMessage;
import io.thingshub.entity.Device;
import io.thingshub.entity.ScriptInfo;
import io.thingshub.http.interceptor.Interceptor;
import io.thingshub.script.ScriptEngine;
import io.thingshub.script.ScriptEngineFactory;
import io.thingshub.service.DeviceService;
import io.thingshub.service.ScriptService;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

/**
 * <p>
 * Message Transform Interceptor
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class MessageTransformInterceptor implements Interceptor {

	@Override
	public boolean preHandle(HttpServerRequest request, HttpServerResponse response) throws Exception {
		String scriptId = "";// TODO
		ScriptInfo theScript = Broker.getBean(ScriptService.class).getScript(scriptId);
		ScriptEngine scriptEngine = Broker.getBean(ScriptEngineFactory.class).getScriptEngine(theScript.getLang());
		String deviceSn = scriptEngine.invoke(scriptId, String.class, "decode", null);
		if (deviceSn != null && !deviceSn.isBlank()) {
			Device device = Broker.getBean(DeviceService.class).getBySn(deviceSn);
			if (device != null) {
				long ts = System.currentTimeMillis();

				ThingshubMessage inboundMessage = scriptEngine.invoke(device.getProductCode(), ThingshubMessage.class, "decode", null);
			}
		}

		return false;
	}

}