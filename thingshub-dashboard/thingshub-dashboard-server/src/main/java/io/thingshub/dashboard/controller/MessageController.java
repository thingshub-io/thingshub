package io.thingshub.dashboard.controller;

import java.util.HashMap;
import java.util.Map;

import io.thingshub.commons.Page;
import io.thingshub.dashboard.params.MessageRequestParams.QueryDeliveryParams;
import io.thingshub.http.annotation.Controller;
import io.thingshub.http.annotation.RequestMapping;
import io.thingshub.service.InboxService;
import io.thingshub.service.model.Delivery;
import jakarta.inject.Inject;

/**
 * <p>
 * 消息管理
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Controller
public class MessageController {

	@Inject
	private InboxService inboxService;

	@RequestMapping(path = "/message/delivery/query")
	public Page<Delivery> queryDeliveries(QueryDeliveryParams params) {
		Map<String, Object> queryParams = new HashMap<>();

		return inboxService.queryDeliveries(queryParams, params.getPage(), params.getSize());
	}

}