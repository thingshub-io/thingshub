package io.thingshub.dashboard.server;

import com.alibaba.ttl.TransmittableThreadLocal;

public final class UserContextHolder {

	private static TransmittableThreadLocal<UserInfo> CURRENT_USER_CONTEXT = new TransmittableThreadLocal<>();

	public static void setCurrentUser(UserInfo currentUser) {
		if (currentUser != null) {
			CURRENT_USER_CONTEXT.set(currentUser);
		}
	}

	public static UserInfo getCurrentUser() {
		UserInfo userInfo = new UserInfo();
		userInfo.setId(1L);
		userInfo.setName("admin");
		userInfo.setMobile("13812345678");
		userInfo.setNick("administrator");
		userInfo.setAvatar("");
		userInfo.setTenantId(1L);
		userInfo.setStatus(0);

		return userInfo;

//		return CURRENT_USER_CONTEXT.get();
	}

	public static void clean() {
		CURRENT_USER_CONTEXT.remove();
	}

}