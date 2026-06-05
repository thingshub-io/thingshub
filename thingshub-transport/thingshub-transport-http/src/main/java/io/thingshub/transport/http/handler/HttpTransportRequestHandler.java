package io.thingshub.transport.http.handler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Maps;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.thingshub.Broker;
import io.thingshub.commons.Result;
import io.thingshub.commons.ServiceException;
import io.thingshub.commons.SysException;
import io.thingshub.commons.ThingshubMessage;
import io.thingshub.entity.Device;
import io.thingshub.entity.ScriptInfo;
import io.thingshub.http.MultipartFile;
import io.thingshub.http.MultipartHttpRequest;
import io.thingshub.http.validation.ValidationException;
import io.thingshub.ioc.Component;
import io.thingshub.script.ScriptEngine;
import io.thingshub.script.ScriptEngineFactory;
import io.thingshub.service.DeviceService;
import io.thingshub.service.ScriptService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

/**
 * <p>
 * HTTP Transport Server Request Handler
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
@Component
public class HttpTransportRequestHandler implements Consumer<HttpServerRoutes> {

	private BiFunction<String, Class<?>, ?> jsonConverter = (json, clazz) -> JSON.parseObject(json, clazz);

	@Override
	public void accept(HttpServerRoutes routes) {
		routes.get("/**", this::handleRequest);
		routes.post("/**", this::handleRequest);

	}

	@SuppressWarnings("incomplete-switch")
	private Publisher<Void> handleRequest(HttpServerRequest request, HttpServerResponse response) {
		if (request.method().name().equalsIgnoreCase("GET")) {
			QueryStringDecoder query = new QueryStringDecoder(request.uri());
			Map<String, List<String>> queryParams0 = query.parameters();

			Map<String, Object> queryParams = Maps.newHashMap();
			if (queryParams0 != null) {
				queryParams0.forEach((k, v) -> queryParams.put(k, v.get(0)));
			}

			return request.receive().then().doOnNext(v -> {
				try {
					Object ret = null;

					// TODO
					transformMessage(queryParams);

					String result = JSON.toJSONString(Result.success(ret));
					response.sendString(Mono.just(result), StandardCharsets.UTF_8).then().subscribe();
				} catch (Exception e) {
					log.error("", e);

					if (e instanceof ValidationException ve) {
						response.status(400).sendString(Mono.just(JSON.toJSONString(Result.error(400, ve.getMessage()))), StandardCharsets.UTF_8).then()
								.subscribe();
					} else if (e instanceof InvocationTargetException ite) {
						Throwable t = ite.getTargetException();

						if (t instanceof ServiceException svre) {
							response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(500, svre.getMessage()))), StandardCharsets.UTF_8).then()
									.subscribe();
						} else if (t instanceof SysException se) {
							response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(504, se.getMessage()))), StandardCharsets.UTF_8).then()
									.subscribe();
						}
					} else {
						response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(504, "system error"))), StandardCharsets.UTF_8).then()
								.subscribe();
					}
				}
			}).then();
		} else if (request.method().name().equalsIgnoreCase("POST")) {
			if (request.isMultipart()) {
				MultipartHttpRequest multipartHttpRequest = new MultipartHttpRequest(request);

				return multipartHttpRequest.receiveForm().flatMap(data -> {
					if (data.isCompleted()) {
						try {
							switch (data.getHttpDataType()) {
							case FileUpload -> {
								FileUpload uploadData = (FileUpload) data;
								MultipartFile multipartFile = new MultipartFile(uploadData.getName(), uploadData.getFilename(), uploadData.getContentType(),
										uploadData.get());
								multipartHttpRequest.addFile(uploadData.getName(), multipartFile);

								return Mono.just(multipartHttpRequest);
							}
							case Attribute -> {
								Attribute attributeData = (Attribute) data;
								multipartHttpRequest.addParameter(attributeData.getName(), attributeData.getValue().toString());
							}
							}
						} catch (IOException e) {
							log.error("", e);
						}
					}

					return Mono.just(multipartHttpRequest);
				}).then(Mono.defer(() -> {
					Map<String, Object> requestParams = Maps.newHashMap();
					Map<String, String[]> theParametersMap = multipartHttpRequest.getParameterMap();
					if (theParametersMap != null) {
						theParametersMap.forEach((key, values) -> {
							if (values != null && values.length > 0) {
								requestParams.put(key, values[0]);
							}
						});
					}
					Map<String, MultipartFile[]> theFilesMap = multipartHttpRequest.getMultiFileMap();
					if (theFilesMap != null) {
						theFilesMap.forEach((key, values) -> {
							if (values != null && values.length > 0) {
								requestParams.put(key, values);
							}
						});
					}

					try {
						Object ret = null;

						// TODO
						transformMessage(requestParams);

						String result = JSON.toJSONString(Result.success(ret));
						response.sendString(Mono.just(result)).then().subscribe();
//						}
					} catch (Exception e) {
						log.error("", e);

						if (e instanceof ValidationException) {
							ValidationException ve = (ValidationException) e;
							response.status(400).sendString(Mono.just(JSON.toJSONString(Result.error(400, ve.getMessage()))), StandardCharsets.UTF_8).then()
									.subscribe();
						} else if (e instanceof InvocationTargetException) {
							InvocationTargetException ite = (InvocationTargetException) e;
							Throwable t = ite.getTargetException();

							if (t instanceof ServiceException) {
								ServiceException svre = (ServiceException) e;
								response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(500, svre.getMessage()))), StandardCharsets.UTF_8)
										.then().subscribe();
							} else if (t instanceof SysException) {
								SysException se = (SysException) e;
								response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(500, se.getMessage()))), StandardCharsets.UTF_8).then()
										.subscribe();
							}
						} else {
							response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(504, "system error"))), StandardCharsets.UTF_8).then()
									.subscribe();
						}
					}

					return Mono.empty();
				}).doOnError(t -> {
					log.error("", t);

					response.sendString(Mono.just(JSON.toJSONString(Result.error(504, "system error"))), StandardCharsets.UTF_8).then().subscribe();
				}).then());
//			} else if (request.isFormUrlencoded()) {DataBufferODO
			} else {
				return request.receive().retain().aggregate().asString(StandardCharsets.UTF_8).map(body -> {
					Object bodyObj = null;
//					bodyObj = jsonConverter.apply(body, methodWrapper.getParameters().get("requestBody").getType());

					return bodyObj;
				}).doOnNext(m -> {
					try {
						Object ret = null;

						// TODO
						transformMessage(m);

						String result = JSON.toJSONString(Result.success(ret));
						response.sendString(Mono.just(result), StandardCharsets.UTF_8).then().subscribe();
					} catch (Exception e) {
						log.error("", e);

						if (e instanceof ValidationException) {
							ValidationException ve = (ValidationException) e;
							response.status(400).sendString(Mono.just(JSON.toJSONString(Result.error(400, ve.getMessage()))), StandardCharsets.UTF_8).then()
									.subscribe();
						} else if (e instanceof InvocationTargetException) {
							InvocationTargetException ite = (InvocationTargetException) e;
							Throwable t = ite.getTargetException();

							if (t instanceof ServiceException) {
								ServiceException svre = (ServiceException) e;
								response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(500, svre.getMessage()))), StandardCharsets.UTF_8)
										.then().subscribe();
							} else if (t instanceof SysException) {
								SysException se = (SysException) e;
								response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(500, se.getMessage()))), StandardCharsets.UTF_8).then()
										.subscribe();
							}
						} else {
							response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(504, "system error"))), StandardCharsets.UTF_8).then()
									.subscribe();
						}
					}
				}).doOnError(t -> {
					log.error("", t);

					response.sendString(Mono.just(JSON.toJSONString(Result.error(504, "system error"))), StandardCharsets.UTF_8).then().subscribe();
				}).then();
			}
		} else {
			return Mono.empty();
		}
	}

	private void transformMessage(Object message) {
		String scriptId = "";// TODO
		ScriptInfo theScript = Broker.getBean(ScriptService.class).getScript(scriptId);
		ScriptEngine scriptEngine = Broker.getBean(ScriptEngineFactory.class).getScriptEngine(theScript.getLang());
		String deviceSn = scriptEngine.invoke(scriptId, String.class, "decode", null);
		if (deviceSn != null && !deviceSn.isBlank()) {
			Device device = Broker.getBean(DeviceService.class).getBySn(deviceSn);
			if (device != null) {
				long ts = System.currentTimeMillis();

				ThingshubMessage inboundMessage = scriptEngine.invoke(device.getProductCode(), ThingshubMessage.class, "decode", null);
			}
		}
	}

}
