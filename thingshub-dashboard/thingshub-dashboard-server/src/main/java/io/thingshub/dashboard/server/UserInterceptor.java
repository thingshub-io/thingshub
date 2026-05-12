package io.thingshub.dashboard.server;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson2.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import cn.hutool.core.text.AntPathMatcher;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.thingshub.http.interceptor.Interceptor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

/**
 * <p>
 * User Interceptor
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class UserInterceptor implements Interceptor {

	public static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

	private static final Cache<Long, UserInfo> userCache = Caffeine.newBuilder().expireAfterWrite(24 * 60 * 60, TimeUnit.SECONDS).maximumSize(10_000).build();

	@Override
	public boolean preHandle(HttpServerRequest request, HttpServerResponse response) throws Exception {
		if (request.path().equals("") || ANT_PATH_MATCHER.match("internal/**", request.path()) || ANT_PATH_MATCHER.match("**/*.html", request.path())
				|| ANT_PATH_MATCHER.match("**/*.js", request.path()) || ANT_PATH_MATCHER.match("**/*.css", request.path())
				|| ANT_PATH_MATCHER.match("**/*.png", request.path()) || ANT_PATH_MATCHER.match("**/*.jpg", request.path())
				|| ANT_PATH_MATCHER.match("**/*.jpeg", request.path())) {
			return true;
		}

//		String userId = request.requestHeaders().get("X-User-Id");
		String userId = "1";
		if (userId != null) {
			UserInfo currentUser = getCurrentUser(Long.valueOf(userId));
			if (currentUser != null) {
				UserContextHolder.setCurrentUser(currentUser);

				return true;
			}
		}

		JSONObject result = new JSONObject();
		result.put("code", HttpResponseStatus.UNAUTHORIZED.code());
		result.put("msg", "未登录");
		result.put("timestamp", System.currentTimeMillis());
		response.status(HttpResponseStatus.UNAUTHORIZED).sendString(Mono.just(result.toJSONString()), StandardCharsets.UTF_8).then().subscribe();

		return false;
	}

	private UserInfo getCurrentUser(Long userId) {
		return userCache.get(userId, uid -> {
			try {
				// TODO get user info

				UserInfo userInfo = new UserInfo();
				userInfo.setId(1L);
				userInfo.setName("admin");
				userInfo.setMobile("13812345678");
				userInfo.setNick("administrator");
				userInfo.setAvatar("");
				userInfo.setTenantId(1L);
				userInfo.setStatus(0);

				return userInfo;
			} catch (Exception e) {
				log.error("获取当前用户信息错误：", e);

				return null;
			}
		});
	}

	private UserInfo userProfile2UserInfo(JSONObject userProfileObj) {
		UserInfo userInfo = new UserInfo();
		userInfo.setId(userProfileObj.getLong("id"));
		userInfo.setName(userProfileObj.getString("name"));
		userInfo.setMobile(userProfileObj.getString("mobile"));
		userInfo.setNick(userProfileObj.getString("nick"));
		userInfo.setAvatar(userProfileObj.getString("avatar"));
		userInfo.setTenantId(userProfileObj.getLong("tenantId"));
		userInfo.setStatus(userProfileObj.getInteger("status"));

		if (userProfileObj.getJSONArray("menus") != null) {
			List<String> authorities = new ArrayList<>();
			userProfileObj.getJSONArray("menus").forEach(opt -> {
				authorities.add(((JSONObject) opt).getString("authorityId"));
			});

			userInfo.setAuthorities(authorities);
		}

		return userInfo;
	}

	public void destroy() {
		// TODO
	}

}