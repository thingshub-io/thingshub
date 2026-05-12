package io.thingshub.http.interceptor;

import java.util.Set;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

@Slf4j
public class InterceptorChain {

	@Getter
	private Set<Interceptor> interceptors;

	public InterceptorChain(Set<Interceptor> interceptors) {
		this.interceptors = interceptors;
	}

	public boolean applyPreHandle(HttpServerRequest request, HttpServerResponse response) throws Exception {
		if (interceptors != null) {
			for (Interceptor interceptor : interceptors) {
				if (!interceptor.preHandle(request, response)) {
					triggerAfterCompletion(request, response, null);
					return false;
				}
			}
		}

		return true;
	}

	public void applyPostHandle(HttpServerRequest request, HttpServerResponse response) throws Exception {
		if (interceptors != null) {
			for (Interceptor interceptor : interceptors) {
				interceptor.postHandle(request, response);
			}
		}
	}

	public void triggerAfterCompletion(HttpServerRequest request, HttpServerResponse response, Exception ex) {
		if (interceptors != null) {
			for (Interceptor interceptor : interceptors) {
				try {
					interceptor.afterCompletion(request, response, ex);
				} catch (Throwable t) {
					log.error("HandlerInterceptor.afterCompletion threw exception", t);
				}
			}
		}
	}

}
