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
		return CURRENT_USER_CONTEXT.get();
	}

	public static void clean() {
		CURRENT_USER_CONTEXT.remove();
	}

}