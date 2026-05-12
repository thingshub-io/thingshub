package io.thingshub.dashboard.server;

import io.thingshub.ClassPathResource;
import io.thingshub.http.handler.RequestHandler;
import io.thingshub.ioc.Component;
import reactor.netty.http.server.HttpServerRoutes;

/**
 * <p>
 * Server Request Handler
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Component
public class DashboardRequestHandler extends RequestHandler {

	public static final String STATIC_ROOT_PATH = "/static/";

	public DashboardRequestHandler() {
		super("io.thingshub.dashboard");
	}

	@Override
	protected void attachRoute(HttpServerRoutes routes) {
		routes.get("/static/**", (request, response) -> {
			String path = request.path();
			if (RequestHandler.ANT_PATH_MATCHER.match("/static/index.html", request.path())) {
				response.addHeader("Content-Type", "text/html");
			} else if (RequestHandler.ANT_PATH_MATCHER.match("/static/assets/**/*.js", request.path())) {
				response.addHeader("Content-Type", "application/javascript;charset=UTF-8");
			} else if (RequestHandler.ANT_PATH_MATCHER.match("/static/assets/**/*.css", request.path())) {
				response.addHeader("Content-Type", "text/css;charset=UTF-8");
			} else if (RequestHandler.ANT_PATH_MATCHER.match("/static/assets/**/*.png", request.path())) {
				response.addHeader("Content-Type", "image/png");
			} else if (RequestHandler.ANT_PATH_MATCHER.match("/static/assets/**/*.jpg", request.path())) {
				response.addHeader("Content-Type", "image/jpeg");
			} else if (RequestHandler.ANT_PATH_MATCHER.match("/static/assets/**/*.jpeg", request.path())) {
				response.addHeader("Content-Type", "image/jpeg");
			} else {
				path = "/static/index.html";
			}
			return response.send(ClassPathResource.readFile(path)).then();
		});
	}

}
