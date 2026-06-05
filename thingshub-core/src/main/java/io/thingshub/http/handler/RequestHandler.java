package io.thingshub.http.handler;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.validation.groups.Default;

import org.reactivestreams.Publisher;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cn.hutool.core.text.AntPathMatcher;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.thingshub.Broker;
import io.thingshub.commons.Result;
import io.thingshub.commons.ServiceException;
import io.thingshub.commons.SysException;
import io.thingshub.http.HttpMethod;
import io.thingshub.http.MethodParameter;
import io.thingshub.http.MethodWrapper;
import io.thingshub.http.MultipartFile;
import io.thingshub.http.MultipartHttpRequest;
import io.thingshub.http.annotation.Controller;
import io.thingshub.http.annotation.RequestBody;
import io.thingshub.http.annotation.RequestMapping;
import io.thingshub.http.annotation.RequestParam;
import io.thingshub.http.annotation.Validated;
import io.thingshub.http.interceptor.Interceptor;
import io.thingshub.http.interceptor.InterceptorChain;
import io.thingshub.http.validation.ValidationException;
import io.thingshub.http.validation.ValidationUtils;
import io.thingshub.utils.TypeUtils;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

/**
 * <p>
 * Request Handler
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public abstract class RequestHandler implements Consumer<HttpServerRoutes> {

	public static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

	private static final String INTERNAL_SERVICE_API_URL_PATTERN = "**/internal/**";

	private final Map<String, MethodWrapper> REQUEST_METHODS = new HashMap<>();

	private final Map<String, String> PATH_METHOD_MAPPING = new HashMap<>();

	private BiFunction<String, Type, ?> jsonConverter = (json, type) -> JSON.parseObject(json, type);

	@Getter
	private InterceptorChain interceptorChain;

	public RequestHandler(String scanBasePackages) {
		Configuration configuration = new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage(scanBasePackages))
				.filterInputsBy(new FilterBuilder().includePackage(scanBasePackages));
		Reflections reflections = new Reflections(configuration);

		Set<Interceptor> interceptors = Sets.newHashSet();
		Set<Class<? extends Interceptor>> interceptorClazzs = reflections.getSubTypesOf(Interceptor.class);
		if (interceptorClazzs != null) {
			interceptorClazzs.forEach(interceptorClazz -> {
				try {
					if (!interceptorClazz.isInterface() && !Modifier.isAbstract(interceptorClazz.getModifiers())) {
						interceptors.add(interceptorClazz.getDeclaredConstructor().newInstance());
					}
				} catch (Exception e) {
					log.error("", e);
				}
			});
		}
		this.interceptorChain = new InterceptorChain(interceptors);

		Set<Class<?>> controllerClazzs = reflections.get(Scanners.TypesAnnotated.with(Controller.class).asClass());
		if (controllerClazzs != null) {
			ClassPool clazzPool = ClassPool.getDefault();
			for (Class<?> controllerClazz : controllerClazzs) {
				try {
					CtClass ctClass = clazzPool.getCtClass(controllerClazz.getName());
					Method[] methods = controllerClazz.getMethods();
					for (Method method : methods) {
						if (!method.isAnnotationPresent(RequestMapping.class)) {
							continue;
						}

						RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
						String[] paths = requestMapping.path() != null ? requestMapping.path() : requestMapping.value();
						if (paths != null && paths.length > 0) {
							for (String path : paths) {
								if (PATH_METHOD_MAPPING.get(path) != null) {
									throw new SysException(String.format("Ambiguous mapping. Path %s has already mapped to the method %s", path,
											controllerClazz.getCanonicalName() + "." + method.getName()));
								}

								PATH_METHOD_MAPPING.put(path, controllerClazz.getCanonicalName() + "." + method.getName());
							}
						}

//						PATH_METHOD_MAPPING.put(controllerClazz.getCanonicalName() + "." + method.getName(), methodWrapper);

						final Class<?>[] parameterTypes = method.getParameterTypes();
						final Type[] genericTypes = method.getGenericParameterTypes();

						MethodWrapper methodWrapper = new MethodWrapper();
						methodWrapper.setMethod(method);
						methodWrapper.setRequestMapping(requestMapping);
						methodWrapper.setParameters(new LinkedHashMap<>());

						CtMethod ctMethod = ctClass.getDeclaredMethod(method.getName());
						int len = parameterTypes.length;
						int pos = Modifier.isStatic(ctMethod.getModifiers()) ? 0 : 1;

						MethodInfo methodInfo = ctMethod.getMethodInfo();
						Object[][] allParameterAnnotations = ctMethod.getParameterAnnotations();
						CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
						LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);

						if (attr != null) {
							for (int i = 0; i < len; i++) {
								String parameterName = attr.variableName(i + pos);
								Class<?> parameterClazz = parameterTypes[i];
								Type genericTypeType = genericTypes[i];
								Class<?>[] parameterValidationGroups = new Class[] { Default.class };
								Object[] parameterAnnotations = allParameterAnnotations[i];
								String requestParamName = parameterName;
								boolean needValidation = true;
								boolean hasRequestBody = false;

								if (parameterAnnotations != null && parameterAnnotations.length > 0) {
									for (Object parameterAnnotation : parameterAnnotations) {
										Class<? extends Annotation> annotationType = ((Annotation) parameterAnnotation).annotationType();

										if (annotationType == Validated.class) {
											Validated validatedAnnotation = (Validated) parameterAnnotation;
											if (validatedAnnotation.value() != null && validatedAnnotation.value().length > 0) {
												parameterValidationGroups = validatedAnnotation.value();
											}
										} else if (annotationType == RequestParam.class) {
											RequestParam requestParamAnnotation = (RequestParam) parameterAnnotation;
											if (requestParamAnnotation.name() != null && !requestParamAnnotation.name().isBlank()) {
												requestParamName = requestParamAnnotation.name();
											} else if (requestParamAnnotation.value() != null && !requestParamAnnotation.value().isBlank()) {
												requestParamName = requestParamAnnotation.value();
											}

											needValidation = requestParamAnnotation.required();
										} else if (annotationType == RequestBody.class) {
											hasRequestBody = true;
										}
									}
								}

								Field[] parameterObjectFields = getAllFieldsOfClass(parameterClazz);

								MethodParameter methodParameter = new MethodParameter();
								methodParameter.setRequestParamName(requestParamName);
								methodParameter.setName(parameterName);
//								methodParameter.setClazz(parameterClazz);
								methodParameter.setType(genericTypeType);
								methodParameter.setParameterObjectFields(parameterObjectFields);
								methodParameter.setValidationGroups(parameterValidationGroups);
								methodParameter.setNeedValidation(needValidation);

								methodWrapper.getParameters().put(parameterName, methodParameter);
								if (hasRequestBody) {
									methodWrapper.getParameters().put("requestBody", methodParameter);
								}
							}
						}

						REQUEST_METHODS.put(controllerClazz.getCanonicalName() + "." + method.getName(), methodWrapper);
					}
				} catch (Exception e) {
					log.error("", e);

					System.exit(0);
				}
			}
		}
	}

	private Field[] getAllFieldsOfClass(Class<?> clazz) {
		Field[] fields = clazz.getDeclaredFields();
		Class<?> parent = clazz.getSuperclass();
		if (parent != null) {
			Field[] parentFields = getAllFieldsOfClass(parent);
			Field[] allFields = new Field[fields.length + parentFields.length];
			System.arraycopy(fields, 0, allFields, 0, fields.length);
			System.arraycopy(parentFields, 0, allFields, fields.length, parentFields.length);
			fields = allFields;
		}

		return fields;
	}

	@Override
	public void accept(HttpServerRoutes routes) {
		REQUEST_METHODS.forEach((methodName, methodWrapper) -> {
			BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> requestHandler = (request, response) -> this.handleRequest(request,
					response, methodWrapper);

			for (HttpMethod httpMethod : methodWrapper.getRequestMapping().method()) {
				String[] paths = methodWrapper.getRequestMapping().path() != null ? methodWrapper.getRequestMapping().path() : methodWrapper.getRequestMapping().value();
				for (String path : paths) {
					switch (httpMethod) {
					case OPTIONS -> {
						// TODO
					}
					case HEAD -> {
						// TODO
					}
					case PATCH -> {
						// TODO
					}
					case TRACE -> {
					}
					case PUT -> routes.put(path, requestHandler);
					case POST -> routes.post(path, requestHandler);
					case DELETE -> routes.delete(path, requestHandler);
					case GET -> routes.get(path, requestHandler);
					}
				}
			}
		});

		attachRoute(routes);
	}

	protected void attachRoute(HttpServerRoutes routes) {

	}

	@SuppressWarnings("incomplete-switch")
	private Publisher<Void> handleRequest(HttpServerRequest request, HttpServerResponse response, MethodWrapper methodWrapper) {
		if (methodWrapper.getRequestMapping().headers() != null) {
			// TODO 不符合header要求的返回404
		}
		if (methodWrapper.getRequestMapping().consumes() != null) {
//			httpServerRequest.isMultipart()
			// TODO 不符合consumes要求的返回404
		}
		if (methodWrapper.getRequestMapping().produces() != null) {
			// TODO 不符合produces要求的返回404

			for (String produce : methodWrapper.getRequestMapping().produces()) {
				response.addHeader("Content-Type", produce);
			}
		}

		try {
			if (!interceptorChain.applyPreHandle(request, response)) {
				return Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, null));
			}
		} catch (Exception e) {
			log.error("", e);

			return response.sendString(Mono.just(JSON.toJSONString(Result.error(504, "system error"))), StandardCharsets.UTF_8)
					.then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, e))).then();
		}

		if (request.method().name().equalsIgnoreCase("GET")) {
			QueryStringDecoder query = new QueryStringDecoder(request.uri());
			Map<String, List<String>> queryParams0 = query.parameters();

			Map<String, Object> queryParams = Maps.newHashMap();
			if (queryParams0 != null) {
				queryParams0.forEach((k, v) -> queryParams.put(k, v.get(0)));
			}

			return request.receive().then(Mono.just(bindRequestParamsToMethodParameters(request, response, methodWrapper, queryParams))).doOnNext(methodParams -> {
				try {
					Object ret = null;
					if (methodWrapper.getParameters() != null) {
						for (Entry<String, MethodParameter> entry : methodWrapper.getParameters().entrySet()) {
							if (entry.getKey().equals("requestBody")) {
								continue;
							}

							if (entry.getValue().isNeedValidation()) {
								Object methodParamValue = methodParams.get(entry.getKey());
								if (methodParamValue == null) {
									throw new ValidationException(entry.getValue().getRequestParamName().concat("不能为空"));
								}

								ValidationUtils.validate(methodParams.get(entry.getKey()), entry.getValue().getValidationGroups());
							}

						}

						ret = methodWrapper.getMethod().invoke(Broker.getBean(methodWrapper.getMethod().getDeclaringClass()), methodParams.values().toArray());
					} else {
						ret = methodWrapper.getMethod().invoke(Broker.getBean(methodWrapper.getMethod().getDeclaringClass()));
					}

					interceptorChain.applyPostHandle(request, response);

//								if (methodWrapper.getRequestMapping().produces()[0].indexOf("application/json") > -1) {
					String result = ANT_PATH_MATCHER.match(INTERNAL_SERVICE_API_URL_PATTERN, request.path()) ? JSON.toJSONString(ret) : JSON.toJSONString(Result.success(ret));
					response.sendString(Mono.just(result), StandardCharsets.UTF_8).then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, null))).then()
							.subscribe();
//								}
				} catch (Exception e) {
					log.error("", e);

					if (e instanceof ValidationException ve) {
						response.status(400).sendString(Mono.just(JSON.toJSONString(Result.error(400, ve.getMessage()))), StandardCharsets.UTF_8)
								.then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, ve))).then().subscribe();
					} else if (e instanceof InvocationTargetException ite) {
						Throwable t = ite.getTargetException();

						if (t instanceof ServiceException svre) {
							response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(500, svre.getMessage()))), StandardCharsets.UTF_8)
									.then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, svre))).then().subscribe();
						} else if (t instanceof SysException se) {
							response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(504, se.getMessage()))), StandardCharsets.UTF_8)
									.then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, se))).then().subscribe();
						}
					} else {
						response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(504, "system error"))), StandardCharsets.UTF_8)
								.then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, e))).then().subscribe();
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
								MultipartFile multipartFile = new MultipartFile(uploadData.getName(), uploadData.getFilename(), uploadData.getContentType(), uploadData.get());
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
					Map<String, Object> multipartMethodParams = bindRequestParamsToMethodParameters(multipartHttpRequest, response, methodWrapper, requestParams);

					try {
						Object ret = null;
						if (methodWrapper.getParameters() != null) {
							for (Entry<String, MethodParameter> entry : methodWrapper.getParameters().entrySet()) {
								if (entry.getKey().equals("requestBody")) {
									continue;
								}

								if (entry.getValue().isNeedValidation()) {
									Object methodParamValue = multipartMethodParams.get(entry.getKey());
									if (methodParamValue == null) {
										throw new ValidationException(entry.getValue().getRequestParamName().concat("不能为空"));
									}

									ValidationUtils.validate(multipartMethodParams.get(entry.getKey()), entry.getValue().getValidationGroups());
								}
							}

							ret = methodWrapper.getMethod().invoke(Broker.getBean(methodWrapper.getMethod().getDeclaringClass()), multipartMethodParams.values().toArray());
						} else {
							ret = methodWrapper.getMethod().invoke(Broker.getBean(methodWrapper.getMethod().getDeclaringClass()));
						}

						interceptorChain.applyPostHandle(request, response);

//						if (methodWrapper.getRequestMapping().produces()[0].indexOf("application/json") > -1) {
						String result = ANT_PATH_MATCHER.match(INTERNAL_SERVICE_API_URL_PATTERN, request.path()) ? JSON.toJSONString(ret) : JSON.toJSONString(Result.success(ret));
						response.sendString(Mono.just(result)).then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, null))).then().subscribe();
//						}
					} catch (Exception e) {
						log.error("", e);

						if (e instanceof ValidationException ve) {
							response.status(400).sendString(Mono.just(JSON.toJSONString(Result.error(400, ve.getMessage()))), StandardCharsets.UTF_8)
									.then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, ve))).then().subscribe();
						} else if (e instanceof InvocationTargetException ite) {
							Throwable t = ite.getTargetException();

							if (t instanceof ServiceException svre) {
								response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(500, svre.getMessage()))), StandardCharsets.UTF_8)
										.then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, svre))).then().subscribe();
							} else if (t instanceof SysException syse) {
								response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(500, syse.getMessage()))), StandardCharsets.UTF_8)
										.then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, syse))).then().subscribe();
							}
						} else {
							response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(504, "system error"))), StandardCharsets.UTF_8)
									.then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, e))).then().subscribe();
						}
					}

					return Mono.empty();
				}).doOnError(t -> {
					log.error("", t);

					response.sendString(Mono.just(JSON.toJSONString(Result.error(504, "system error"))), StandardCharsets.UTF_8)
							.then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, new SysException(t)))).then().subscribe();
				}).then());
//			} else if (request.isFormUrlencoded()) {DataBufferODO
			} else {
				return request.receive().retain().aggregate().asString(StandardCharsets.UTF_8).map(body -> {
					Map<String, Object> methodParams = new LinkedHashMap<>();
					if (methodWrapper.getParameters().get("requestBody") != null) {
						Object bodyObj = jsonConverter.apply(body, methodWrapper.getParameters().get("requestBody").getType());
						methodParams.put(methodWrapper.getParameters().get("requestBody").getName(), bodyObj);
					}

					return methodParams;
				}).doOnNext(methodParams -> {
					try {
						Object ret = null;

						if (methodWrapper.getParameters() != null) {
							for (Entry<String, MethodParameter> entry : methodWrapper.getParameters().entrySet()) {
								if (entry.getKey().equals("requestBody")) {
									continue;
								}

								if (entry.getValue().isNeedValidation()) {
									Object methodParamValue = methodParams.get(entry.getKey());
									if (methodParamValue == null) {
										throw new ValidationException(entry.getValue().getRequestParamName().concat("不能为空"));
									}

									ValidationUtils.validate(methodParams.get(entry.getKey()), entry.getValue().getValidationGroups());
								}
							}

							for (Entry<String, Object> entry : methodParams.entrySet()) {
								if (methodWrapper.getParameters().get(entry.getKey()) != null) {
									ValidationUtils.validate(entry.getValue(), methodWrapper.getParameters().get(entry.getKey()).getValidationGroups());
								}
							}

							ret = methodWrapper.getMethod().invoke(Broker.getBean(methodWrapper.getMethod().getDeclaringClass()), methodParams.values().toArray());
						} else {
							ret = methodWrapper.getMethod().invoke(Broker.getBean(methodWrapper.getMethod().getDeclaringClass()));
						}

						interceptorChain.applyPostHandle(request, response);

//						if (methodWrapper.getRequestMapping().produces()[0].indexOf("application/json") > -1) {
						String result = ANT_PATH_MATCHER.match(INTERNAL_SERVICE_API_URL_PATTERN, request.path()) ? JSON.toJSONString(ret) : JSON.toJSONString(Result.success(ret));
						response.sendString(Mono.just(result), StandardCharsets.UTF_8).then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, null))).then()
								.subscribe();
//						}
					} catch (Exception e) {
						log.error("", e);

						if (e instanceof ValidationException ve) {
							response.status(400).sendString(Mono.just(JSON.toJSONString(Result.error(400, ve.getMessage()))), StandardCharsets.UTF_8)
									.then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, ve))).then().subscribe();
						} else if (e instanceof InvocationTargetException ite) {
							Throwable t = ite.getTargetException();

							if (t instanceof ServiceException svre) {
								response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(500, svre.getMessage()))), StandardCharsets.UTF_8)
										.then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, svre))).then().subscribe();
							} else if (t instanceof SysException syse) {
								response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(500, syse.getMessage()))), StandardCharsets.UTF_8)
										.then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, syse))).then().subscribe();
							}
						} else {
							response.status(500).sendString(Mono.just(JSON.toJSONString(Result.error(504, "system error"))), StandardCharsets.UTF_8)
									.then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, e))).then().subscribe();
						}
					}
				}).doOnError(t -> {
					log.error("", t);

					response.sendString(Mono.just(JSON.toJSONString(Result.error(504, "system error"))), StandardCharsets.UTF_8)
							.then(Mono.from(v -> interceptorChain.triggerAfterCompletion(request, response, new SysException(t)))).then().subscribe();
				}).then();
			}
		} else {
			return Mono.empty();
		}
	}

	private Map<String, Object> bindRequestParamsToMethodParameters(HttpServerRequest request, HttpServerResponse response, MethodWrapper methodWrapper,
			Map<String, Object> requestParams) {
		Map<String, Object> methodParams = new LinkedHashMap<>();

		methodWrapper.getParameters().forEach((parameterName, methodParameter) -> {
			if (parameterName.equals("requestBody")) {
				return;
			}

			Object paramValue = (requestParams != null && requestParams.get(methodParameter.getRequestParamName()) != null)
					? requestParams.get(methodParameter.getRequestParamName())
					: null;

			switch (methodParameter.getType().getTypeName()) {
			case "java.lang.String" -> {
				methodParams.put(parameterName, paramValue);
			}
			case "long" -> {
				if (paramValue != null) {
					methodParams.put(parameterName, Long.parseLong(paramValue.toString()));
				} else {
					methodParams.put(parameterName, null);
				}
			}
			case "java.lang.Long" -> {
				if (paramValue != null) {
					methodParams.put(parameterName, Long.valueOf(paramValue.toString()));
				} else {
					methodParams.put(parameterName, null);
				}
			}
			case "int" -> {
				if (paramValue != null) {
					methodParams.put(parameterName, Integer.parseInt(paramValue.toString()));
				} else {
					methodParams.put(parameterName, null);
				}
			}
			case "java.lang.Integer" -> {
				if (paramValue != null) {
					methodParams.put(parameterName, Integer.valueOf(paramValue.toString()));
				} else {
					methodParams.put(parameterName, null);
				}
			}
			case "short" -> {
				if (paramValue != null) {
					methodParams.put(parameterName, Short.parseShort(paramValue.toString()));
				} else {
					methodParams.put(parameterName, null);
				}
			}
			case "java.lang.Short" -> {
				if (paramValue != null) {
					methodParams.put(parameterName, Short.valueOf(paramValue.toString()));
				} else {
					methodParams.put(parameterName, null);
				}
			}
			case "byte" -> {
				if (paramValue != null) {
					methodParams.put(parameterName, Byte.parseByte(paramValue.toString()));
				} else {
					methodParams.put(parameterName, null);
				}
			}
			case "java.lang.Byte" -> {
				if (paramValue != null) {
					methodParams.put(parameterName, Byte.valueOf(paramValue.toString()));
				} else {
					methodParams.put(parameterName, null);
				}
			}
			case "boolean" -> {
				if (paramValue != null) {
					methodParams.put(parameterName, Boolean.parseBoolean(paramValue.toString()));
				} else {
					methodParams.put(parameterName, null);
				}
			}
			case "java.lang.Boolean" -> {
				if (paramValue != null) {
					methodParams.put(parameterName, Boolean.valueOf(paramValue.toString()));
				} else {
					methodParams.put(parameterName, null);
				}
			}
			case "float" -> {
				if (paramValue != null) {
					methodParams.put(parameterName, Float.parseFloat(paramValue.toString()));
				} else {
					methodParams.put(parameterName, null);
				}
			}
			case "java.lang.Float" -> {
				if (paramValue != null) {
					methodParams.put(parameterName, Float.valueOf(paramValue.toString()));
				} else {
					methodParams.put(parameterName, null);
				}
			}
			case "double" -> {
				if (paramValue != null) {
					methodParams.put(parameterName, Double.parseDouble(paramValue.toString()));
				} else {
					methodParams.put(parameterName, null);
				}
			}
			case "java.lang.Double" -> {
				if (paramValue != null) {
					methodParams.put(parameterName, Double.valueOf(paramValue.toString()));
				} else {
					methodParams.put(parameterName, null);
				}
			}
			case "java.math.BigDecimal" -> {
				if (paramValue != null) {
					methodParams.put(parameterName, new BigDecimal(paramValue.toString()));
				} else {
					methodParams.put(parameterName, null);
				}
			}
			case "reactor.netty.http.server.HttpServerRequest" -> {
				methodParams.put(parameterName, request);
			}
			case "reactor.netty.http.server.HttpServerResponse" -> {
				methodParams.put(parameterName, response);
			}
			case "io.thingshub.transport.http.MultipartFile[]" -> {
				if (paramValue != null) {
					methodParams.put(parameterName, paramValue);
				} else {
					methodParams.put(parameterName, null);
				}
			}
			default -> {
				try {
					Object methodParameterInstance = TypeUtils.createInstance(methodParameter.getType());
					Field[] fields = methodParameter.getParameterObjectFields();
					for (Field field : fields) {
						field.setAccessible(true);

						String fieldValue = null;
						if (requestParams != null) {
							fieldValue = requestParams.get(field.getName()) != null ? requestParams.get(field.getName()).toString() : null;
						}

						switch (field.getType().getTypeName()) {
						case "java.lang.String" -> {
							if (fieldValue != null) {
								field.set(methodParameterInstance, fieldValue);
							}
						}
						case "long" -> {
							if (fieldValue != null) {
								field.set(methodParameterInstance, Long.parseLong(fieldValue));
							}
						}
						case "java.lang.Long" -> {
							if (fieldValue != null) {
								field.set(methodParameterInstance, Long.valueOf(fieldValue));
							}
						}
						case "int" -> {
							if (fieldValue != null) {
								field.set(methodParameterInstance, Integer.parseInt(fieldValue));
							}
						}
						case "java.lang.Integer" -> {
							if (fieldValue != null) {
								field.set(methodParameterInstance, Integer.valueOf(fieldValue));
							}
						}
						case "short" -> {
							if (fieldValue != null) {
								field.set(methodParameterInstance, Short.parseShort(fieldValue));
							}
						}
						case "java.lang.Short" -> {
							if (fieldValue != null) {
								field.set(methodParameterInstance, Short.valueOf(fieldValue));
							}
						}
						case "byte" -> {
							if (fieldValue != null) {
								field.set(methodParameterInstance, Byte.parseByte(fieldValue));
							}
						}
						case "java.lang.Byte" -> {
							if (fieldValue != null) {
								field.set(methodParameterInstance, Byte.valueOf(fieldValue));
							}
						}
						case "boolean" -> {
							if (fieldValue != null) {
								field.set(methodParameterInstance, Boolean.parseBoolean(fieldValue));
							}
						}
						case "java.lang.Boolean" -> {
							if (fieldValue != null) {
								field.set(methodParameterInstance, Boolean.valueOf(fieldValue));
							}
						}
						case "float" -> {
							if (fieldValue != null) {
								field.set(methodParameterInstance, Float.parseFloat(fieldValue));
							}
						}
						case "java.lang.Float" -> {
							if (fieldValue != null) {
								field.set(methodParameterInstance, Float.valueOf(fieldValue));
							}
						}
						case "double" -> {
							if (fieldValue != null) {
								field.set(methodParameterInstance, Double.parseDouble(fieldValue));
							}
						}
						case "java.lang.Double" -> {
							if (fieldValue != null) {
								field.set(methodParameterInstance, Double.valueOf(fieldValue));
							}
						}
						case "java.math.BigDecimal" -> {
							if (fieldValue != null) {
								field.set(methodParameterInstance, new BigDecimal(fieldValue));
							}
						}
						}
					}
					methodParams.put(parameterName, methodParameterInstance);
				} catch (Exception e) {
					log.error("", e);
				}
			}
			}
		});

		return methodParams;
	}

}
