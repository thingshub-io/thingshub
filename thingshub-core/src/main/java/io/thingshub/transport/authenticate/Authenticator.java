package io.thingshub.transport.authenticate;

import static io.thingshub.service.model.ClientType.CLIENT_USER;
import static io.thingshub.service.model.ClientType.DEVICE;
import static io.thingshub.transport.authenticate.AuthResult.ResultCode.BAD_PASS;
import static io.thingshub.transport.authenticate.AuthResult.ResultCode.NOT_AUTHORIZED;
import static io.thingshub.transport.authenticate.AuthResult.ResultCode.OK;

import io.thingshub.entity.ClientUser;
import io.thingshub.entity.Device;
import io.thingshub.ioc.Service;
import io.thingshub.service.ClientUserService;
import io.thingshub.service.DeviceService;
import io.thingshub.service.model.ClientInfo;
import io.thingshub.service.model.ClientType;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class Authenticator {

	@Inject
	private DeviceService deviceService;

	@Inject
	private ClientUserService clientUserService;

	public AuthResult basicAuth(AuthRequest authRequest) {
		log.debug("Thingshub auth request: {}", authRequest);

		ClientUser clientUser = clientUserService.getClientUser(authRequest.username());
		String username = null, password = null;
		ClientType clientType = null;
		if (clientUser != null) {
			username = clientUser.getUsername();
			password = clientUser.getPassword();
			clientType = CLIENT_USER;
		} else {
			Device device = deviceService.getBySn(authRequest.clientId());
			if (device != null) {
				username = device.getUsername();
				password = device.getPassword();
				clientType = DEVICE;
			}
		}

		if (username == null || password == null) {
			return new AuthResult(new ClientInfo("sys", authRequest.clientId(), null, username), NOT_AUTHORIZED);
		}

		if (username.equals(authRequest.username()) && password.equals(authRequest.password())) {
			return new AuthResult(new ClientInfo("sys", authRequest.clientId(), clientType, username), OK);
		} else {
			return new AuthResult(new ClientInfo("sys", authRequest.clientId(), null, username), BAD_PASS);
		}
	}

	public AuthResult extendedAuth(AuthRequest authRequest) {
		// TODO extended auth
		return basicAuth(authRequest);
	}

}
