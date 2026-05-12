package io.thingshub.microservice;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.client5.http.entity.mime.InputStreamBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.net.URIBuilder;

import com.alibaba.fastjson2.JSON;

import io.thingshub.commons.ServiceException;
import io.thingshub.ioc.Component;
import io.thingshub.plugin.Registry;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 远程服务调用
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Component
@Slf4j
public class ServiceCaller {

	@Inject
	@Nullable
	private Set<Registry> registries;

	private CloseableHttpClient httpClient;

	@PostConstruct
	public void init() {
		httpClient = HttpClients.createDefault();
	}

	public <R> R call(String serviceName, String path, Map<String, String> params, String fileName, InputStream ins, Class<R> rtCls) throws ServiceException {
		ServiceInstance serviceInstance = registries.stream().findFirst().get().selectHealthInstance(serviceName);
		String url = "http://".concat(serviceInstance.getIp()).concat(":").concat(String.valueOf(serviceInstance.getPort())).concat(path);

		try {
			HttpPost request = new HttpPost(url);
			MultipartEntityBuilder multipartBuilder = MultipartEntityBuilder.create();
			for (Entry<String, String> entry : params.entrySet()) {
				multipartBuilder.addPart(entry.getKey(), new StringBody(entry.getValue(), ContentType.TEXT_PLAIN));
			}
			multipartBuilder.addPart("file", new InputStreamBody(ins, ContentType.APPLICATION_OCTET_STREAM, fileName));
			request.setEntity(multipartBuilder.build());

			return httpClient.execute(request, new ServiceResponseHandler<R>(rtCls));
		} catch (IOException e) {
			log.error("调用远程服务{}异常", serviceName);
			log.error("", e);

			throw new ServiceException("服务异常");
		}
	}

	public <R> R call(String serviceName, String path, String method, Map<String, Object> params, Class<R> rtCls) throws ServiceException {
		ServiceInstance serviceInstance = registries.stream().findFirst().get().selectHealthInstance(serviceName);
		String url = "http://".concat(serviceInstance.getIp()).concat(":").concat(String.valueOf(serviceInstance.getPort())).concat(path);

		try {
			ClassicHttpRequest request = null;
			if (method.equalsIgnoreCase("GET")) {
				request = new HttpGet(url);

				if (params != null) {
					List<NameValuePair> paramPairs = new ArrayList<>();
					for (Entry<String, Object> entry : params.entrySet()) {
						if (entry.getValue() == null) {
							continue;
						}

						paramPairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
					}

					try {
						URI uri = new URIBuilder(new URI(url)).addParameters(paramPairs).build();
						request.setUri(uri);
					} catch (URISyntaxException e) {
						log.error("", e);

						throw new ServiceException("服务异常");
					}
				}
			} else if (method.equalsIgnoreCase("POST")) {
				request = new HttpPost(url);

				EntityBuilder entityBuilder = EntityBuilder.create();
				entityBuilder.setContentType(ContentType.APPLICATION_JSON);
				entityBuilder.setText(JSON.toJSONString(params));
				request.setEntity(entityBuilder.build());
			}

			return httpClient.execute(request, new ServiceResponseHandler<R>(rtCls));
		} catch (IOException e) {
			log.error("调用远程服务{}异常", serviceName);
			log.error("", e);

			throw new ServiceException("服务异常");
		}
	}

	public <R> R call(String serviceName, String path, Map<String, Object> params, Class<R> rtCls) throws ServiceException {
		ServiceInstance serviceInstance = registries.stream().findFirst().get().selectHealthInstance(serviceName);
		String url = "http://".concat(serviceInstance.getIp()).concat(":").concat(String.valueOf(serviceInstance.getPort())).concat(path);

		try {
			ClassicHttpRequest request = new HttpPost(url);

			EntityBuilder entityBuilder = EntityBuilder.create();
			entityBuilder.setContentType(ContentType.APPLICATION_JSON);
			entityBuilder.setText(JSON.toJSONString(params));
			request.setEntity(entityBuilder.build());

			return httpClient.execute(request, new ServiceResponseHandler<R>(rtCls));
		} catch (IOException e) {
			log.error("调用远程服务{}异常", serviceName);
			log.error("", e);

			throw new ServiceException("服务异常");
		}
	}

	@PreDestroy
	public void closeClient() {
		if (httpClient != null) {
			httpClient.close(CloseMode.GRACEFUL);
		}
	}

}
