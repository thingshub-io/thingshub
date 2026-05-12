package io.thingshub.http.interceptor;

import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public interface Interceptor {

	default boolean preHandle(HttpServerRequest request, HttpServerResponse response) throws Exception {
		return true;
	}

	default void postHandle(HttpServerRequest request, HttpServerResponse response) throws Exception {
	}

	default void afterCompletion(HttpServerRequest request, HttpServerResponse response, Exception ex) throws Exception {
	}

}
