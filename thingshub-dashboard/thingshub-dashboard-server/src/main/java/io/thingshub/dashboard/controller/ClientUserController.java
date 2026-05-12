package io.thingshub.dashboard.controller;

import java.util.HashMap;
import java.util.Map;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import io.thingshub.commons.IdParam;
import io.thingshub.commons.LongId;
import io.thingshub.commons.ServiceException;
import io.thingshub.commons.model.Page;
import io.thingshub.dashboard.event.ClienntUserProductEvent;
import io.thingshub.dashboard.params.ClientUserRequestParams.BindProductParams;
import io.thingshub.dashboard.params.ClientUserRequestParams.ClientUserFormParams;
import io.thingshub.dashboard.params.ClientUserRequestParams.QueryClientUserParams;
import io.thingshub.dashboard.server.UserContextHolder;
import io.thingshub.dashboard.server.UserInfo;
import io.thingshub.entity.ClientUser;
import io.thingshub.event.ApplicationEventManager;
import io.thingshub.event.ApplicationEventType;
import io.thingshub.http.HttpMethod;
import io.thingshub.http.annotation.Controller;
import io.thingshub.http.annotation.RequestBody;
import io.thingshub.http.annotation.RequestMapping;
import io.thingshub.http.annotation.RequestParam;
import io.thingshub.http.annotation.Validated;
import io.thingshub.service.ClientUserService;
import io.thingshub.service.base.BaseService.AvailableStatus;
import io.thingshub.service.base.BaseService.DeletedStatus;
import jakarta.inject.Inject;

/**
 * <p>
 * Client User Management API
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Controller
public class ClientUserController {

	@Inject
	private ClientUserService clientUserService;

	@Inject
	private ApplicationEventManager eventManager;

	@RequestMapping(path = "/client-user/query")
	public Page<ClientUser> queryClientUsers(@Validated QueryClientUserParams params) {
		Map<String, Object> queryParams = new HashMap<>();

		return clientUserService.queryClientUsers(queryParams, params.getPage(), params.getSize());
	}

	@RequestMapping(path = "/client-user/info")
	public ClientUser getClientUser(@RequestParam Long id) {
		return clientUserService.getClientUser(id);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/client-user/save")
	public LongId saveClientUser(@Validated @RequestBody ClientUserFormParams params) throws ServiceException {
		UserInfo operator = UserContextHolder.getCurrentUser();

		Long clientUserId = params.getId();
		if (clientUserId == null) {
			ClientUser clientUser = new ClientUser();
			BeanUtil.copyProperties(params, clientUser, CopyOptions.create().setIgnoreNullValue(true));

			clientUser.setStatus(AvailableStatus.NORMAL.value());
			clientUser.setDeletedStatus(DeletedStatus.NOT_DELETED.value());
			clientUser.setCreateBy(operator.getName());
			clientUser.setCreateTime(DateUtil.date());
			clientUserId = clientUserService.createClientUser(clientUser);
		} else {
			ClientUser clientUser = clientUserService.getByKey(params.getId());
			if (clientUser == null) {
				throw new ServiceException("无效的客户端用户ID");
			}

			BeanUtil.copyProperties(params, clientUser, CopyOptions.create().setIgnoreNullValue(true));
			clientUserService.updateClientUser(clientUser);
		}

		return new LongId(clientUserId);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/client-user/disable")
	public void disableClientUser(@Validated @RequestBody IdParam idParam) {
		clientUserService.updateStatus(idParam.getId(), AvailableStatus.DISABLED);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/client-user/enable")
	public void enableClientUser(@Validated @RequestBody IdParam idParam) {
		clientUserService.updateStatus(idParam.getId(), AvailableStatus.NORMAL);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/client-user/remove")
	public void removeClientUser(@Validated @RequestBody IdParam idParam) {
		clientUserService.remove(idParam.getId());
	}

	@RequestMapping(method = HttpMethod.POST, path = "/client-user/bind/product")
	public void bindProducts(@Validated @RequestBody BindProductParams params) {
		ClientUser clientUser = clientUserService.bindProducts(params.getId(), params.getProductCodes());

		eventManager.publishEvent(new ClienntUserProductEvent(this, clientUser.getUsername(), params.getProductCodes(), ApplicationEventType.UPDATED.name()));
	}

}