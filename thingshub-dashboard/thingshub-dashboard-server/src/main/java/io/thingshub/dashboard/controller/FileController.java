package io.thingshub.dashboard.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONObject;

import io.thingshub.commons.IdParam;
import io.thingshub.commons.LongId;
import io.thingshub.commons.ServiceException;
import io.thingshub.http.HttpMethod;
import io.thingshub.http.MultipartFile;
import io.thingshub.http.MultipartHttpRequest;
import io.thingshub.http.annotation.Controller;
import io.thingshub.http.annotation.RequestBody;
import io.thingshub.http.annotation.RequestMapping;
import io.thingshub.http.annotation.RequestParam;
import io.thingshub.microservice.ServiceCaller;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.server.HttpServerRequest;

/**
 * <p>
 * 文件上传和下载，调用远程存储服务
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Controller
@Slf4j
public class FileController {

	private final String STORE_SERVICE_NAME = "store";

	private final String UPLOAD_STORE_INTERFACE = "/internal/store/upload";

	private final String GET_STORE_RECORD_INTERFACE = "/internal/store/record";

	private final String GET_STORE_URL_INTERFACE = "/internal/store/access-url";

	private final String REMOVE_STORE_INTERFACE = "/internal/store/remove";

	@Inject
	public ServiceCaller serviceCaller;

	@RequestMapping(method = HttpMethod.POST, path = "/file/upload")
	public LongId uploadSingle(@RequestParam(name = "tenantId", required = false) Long tenantId, @RequestParam("file") MultipartFile[] files)
			throws ServiceException {
		try (InputStream ins = files[0].inputStream();) {
			String fileName = files[0].originalFileName();

			Map<String, String> params = new HashMap<>();
			if (tenantId != null) {
				params.put("tenantId", tenantId.toString());
			}
			params.put("biz", "thingshub");

			Long storeId = serviceCaller.call(STORE_SERVICE_NAME, UPLOAD_STORE_INTERFACE, params, fileName, ins, Long.class);

			return new LongId(storeId);
		} catch (IOException e) {
			log.error("", e);
			throw new ServiceException("处理上传文件异常");
		}

	}

	@RequestMapping(method = HttpMethod.POST, path = "/file/multi-upload")
	public List<Long> uploadMulti(HttpServerRequest request) throws ServiceException {
		MultipartHttpRequest multipartRequest = (MultipartHttpRequest) request;
		Map<String, MultipartFile[]> multifileMap = multipartRequest.getMultiFileMap();
		String tenantId = multipartRequest.getParameter("tenanId");

		List<Long> storeIds = new ArrayList<>(multifileMap.size());
		for (Map.Entry<String, MultipartFile[]> filesEntry : multifileMap.entrySet()) {
			MultipartFile[] theFiles = filesEntry.getValue();
			for (MultipartFile theFile : theFiles) {
				try (InputStream ins = theFile.inputStream();) {
					String fileName = theFile.originalFileName();

					Map<String, String> params = new HashMap<>();
					if (tenantId != null) {
						params.put("tenantId", tenantId.toString());
					}
					params.put("biz", "thingshub");

					Long storeId = serviceCaller.call(STORE_SERVICE_NAME, UPLOAD_STORE_INTERFACE, params, fileName, ins, Long.class);
					storeIds.add(storeId);
				} catch (IOException e) {
					log.error("", e);
					throw new ServiceException("处理上传文件异常");
				}
			}
		}

		return storeIds;

	}

	@RequestMapping(path = "/file/info")
	public JSONObject getFileInfo(@RequestParam Long id) throws ServiceException {
		Map<String, Object> params = new HashMap<>();
		params.put("id", id);

		return serviceCaller.call(STORE_SERVICE_NAME, GET_STORE_RECORD_INTERFACE, "GET", params, JSONObject.class);
	}

	@RequestMapping(path = "/file/access-url")
	public String getAccessUrl(@RequestParam Long id) throws ServiceException {
		Map<String, Object> params = new HashMap<>();
		params.put("id", id);

		return serviceCaller.call(STORE_SERVICE_NAME, GET_STORE_URL_INTERFACE, "GET", params, String.class);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/file/remove")
	public void remove(@RequestBody IdParam param) throws ServiceException {
		Map<String, Object> params = new HashMap<>();
		params.put("id", param.getId());

		serviceCaller.call(STORE_SERVICE_NAME, REMOVE_STORE_INTERFACE, params, Void.class);
	}

}