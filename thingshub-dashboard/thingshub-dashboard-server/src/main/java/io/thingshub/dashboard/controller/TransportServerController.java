package io.thingshub.dashboard.controller;

import java.util.List;

import com.google.common.collect.Lists;

import io.thingshub.Broker;
import io.thingshub.commons.ItemOption;
import io.thingshub.http.annotation.Controller;
import io.thingshub.http.annotation.RequestMapping;

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

	@RequestMapping(path = "/transport-server/options")
	public List<ItemOption> listTransportServerOptions() {
		List<ItemOption> options = Lists.newArrayList();

		for (String serverName : Broker.getServerNames()) {
			options.add(new ItemOption(serverName, serverName));
		}

		return options;
	}

}