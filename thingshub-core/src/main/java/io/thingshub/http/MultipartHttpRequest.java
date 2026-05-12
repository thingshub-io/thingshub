package io.thingshub.http;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.multipart.HttpData;
import reactor.core.publisher.Flux;
import reactor.netty.ByteBufFlux;
import reactor.netty.Connection;
import reactor.netty.http.server.HttpServerFormDecoderProvider.Builder;
import reactor.netty.http.server.HttpServerRequest;

public class MultipartHttpRequest implements HttpServerRequest {

	private final HttpServerRequest httpServerRequest;

	private Map<String, List<MultipartFile>> files = Maps.newHashMap();

	private Map<String, List<String>> parameters = Maps.newHashMap();

	public MultipartHttpRequest(HttpServerRequest httpServerRequest) {
		this.httpServerRequest = httpServerRequest;
	}

	public void addFile(String name, MultipartFile file) {
		files.computeIfAbsent(name, n -> Lists.newArrayList()).add(file);
	}

	public Iterator<String> getFileNames() {
		return files.keySet().iterator();
	}

	public MultipartFile[] getFiles(String name) {
		return files.get(name) != null ? files.get(name).stream().toArray(MultipartFile[]::new) : new MultipartFile[] {};
	}

	public MultipartFile getFile(String name) {
		return files.get(name) != null ? files.get(name).get(0) : null;
	}

	public Map<String, MultipartFile[]> getMultiFileMap() {
		Map<String, MultipartFile[]> multifileMap = Maps.newHashMapWithExpectedSize(files.size());
		files.forEach((key, values) -> {
			if (values != null && !values.isEmpty()) {
				multifileMap.put(key, values.stream().toArray(MultipartFile[]::new));
			}
		});

		return multifileMap;
	}

	public Map<String, MultipartFile> getFileMap() {
		Map<String, MultipartFile> singlefileMap = Maps.newHashMapWithExpectedSize(files.size());
		files.forEach((key, values) -> {
			if (values != null && !values.isEmpty()) {
				singlefileMap.put(key, values.get(0));
			}
		});

		return singlefileMap;
	}

	public void addParameter(String name, String val) {
		parameters.computeIfAbsent(name, n -> Lists.newArrayList()).add(val);
	}

	public String[] getParameters(String name) {
		return parameters.get(name) != null ? parameters.get(name).stream().toArray(String[]::new) : new String[] {};
	}

	public String getParameter(String name) {
		return parameters.get(name) != null ? parameters.get(name).get(0) : null;
	}

	public Map<String, String[]> getParameterMap() {
		Map<String, String[]> multiparamMap = Maps.newHashMapWithExpectedSize(parameters.size());
		parameters.forEach((key, values) -> {
			if (values != null && !values.isEmpty()) {
				multiparamMap.put(key, values.stream().toArray(String[]::new));
			}
		});

		return multiparamMap;
	}

	@Override
	public ByteBufFlux receive() {
		return httpServerRequest.receive();
	}

	@Override
	public Flux<?> receiveObject() {
		return httpServerRequest.receiveObject();
	}

	@Override
	public Map<CharSequence, List<Cookie>> allCookies() {
		return httpServerRequest.allCookies();
	}

	@Override
	public Map<CharSequence, Set<Cookie>> cookies() {
		return httpServerRequest.cookies();
	}

	@Override
	public String fullPath() {
		return httpServerRequest.fullPath();
	}

	@Override
	public String requestId() {
		return httpServerRequest.requestId();
	}

	@Override
	public boolean isKeepAlive() {
		return httpServerRequest.isKeepAlive();
	}

	@Override
	public boolean isWebsocket() {
		return httpServerRequest.isWebsocket();
	}

	@Override
	public HttpMethod method() {
		return httpServerRequest.method();
	}

	@Override
	public String uri() {
		return httpServerRequest.uri();
	}

	@Override
	public HttpVersion version() {
		return httpServerRequest.version();
	}

	@Override
	public SocketAddress connectionHostAddress() {
		return httpServerRequest.connectionHostAddress();
	}

	@Override
	public SocketAddress connectionRemoteAddress() {
		return httpServerRequest.connectionRemoteAddress();
	}

	@Override
	public String scheme() {
		return httpServerRequest.scheme();
	}

	@Override
	public String connectionScheme() {
		return httpServerRequest.connectionScheme();
	}

	@Override
	public String hostName() {
		return httpServerRequest.hostName();
	}

	@Override
	public int hostPort() {
		return httpServerRequest.hostPort();
	}

	@Override
	public HttpServerRequest withConnection(Consumer<? super Connection> withConnection) {
		return httpServerRequest.withConnection(withConnection);
	}

	@Override
	public String param(CharSequence key) {
		return httpServerRequest.param(key);
	}

	@Override
	public Map<String, String> params() {
		return httpServerRequest.params();
	}

	@Override
	public HttpServerRequest paramsResolver(Function<? super String, Map<String, String>> paramsResolver) {
		return httpServerRequest.paramsResolver(paramsResolver);
	}

	@Override
	public boolean isFormUrlencoded() {
		return httpServerRequest.isFormUrlencoded();
	}

	@Override
	public boolean isMultipart() {
		return httpServerRequest.isMultipart();
	}

	@Override
	public Flux<HttpData> receiveForm() {
		return httpServerRequest.receiveForm();
	}

	@Override
	public Flux<HttpData> receiveForm(Consumer<Builder> formDecoderBuilder) {
		return httpServerRequest.receiveForm(formDecoderBuilder);
	}

	@Override
	public InetSocketAddress hostAddress() {
		return httpServerRequest.hostAddress();
	}

	@Override
	public InetSocketAddress remoteAddress() {
		return httpServerRequest.remoteAddress();
	}

	@Override
	public HttpHeaders requestHeaders() {
		return httpServerRequest.requestHeaders();
	}

	@Override
	public String protocol() {
		return httpServerRequest.protocol();
	}

	@Override
	public ZonedDateTime timestamp() {
		return httpServerRequest.timestamp();
	}

}
