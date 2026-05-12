package io.thingshub.microservice;

import java.io.IOException;
import java.math.BigDecimal;

import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import io.thingshub.commons.SysException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceResponseHandler<T> extends AbstractHttpClientResponseHandler<T> {

	private final Class<T> rtClazz;

	public ServiceResponseHandler(Class<T> rtClazz) {
		this.rtClazz = rtClazz;
	}

	@Override
	public T handleEntity(final HttpEntity entity) throws IOException {
		try {
			String resp = EntityUtils.toString(entity);
			if (resp != null) {
				if (resp.indexOf("\"code\"") > -1) {
					JSONObject result = JSON.parseObject(resp);

					return convertType(result.get("data"));
				} else {
					return convertType(resp);
				}
			} else {
				return convertType(resp);
			}
		} catch (final ParseException ex) {
			log.error("", ex);

			throw new SysException("远程服务异常");
		}
	}

	@SuppressWarnings("unchecked")
	private T convertType(Object value) {
		return switch (rtClazz.getTypeName()) {
		case "java.lang.String" -> (T) value;
		case "java.lang.Long" -> (T) Long.valueOf(value.toString());
		case "java.lang.Integer" -> (T) Integer.valueOf(value.toString());
		case "java.lang.Short" -> (T) Short.valueOf(value.toString());
		case "java.lang.Byte" -> (T) Byte.valueOf(value.toString());
		case "java.lang.Boolean" -> (T) Boolean.valueOf(value.toString());
		case "java.lang.Float" -> (T) Float.valueOf(value.toString());
		case "java.lang.Double" -> (T) Double.valueOf(value.toString());
		case "java.math.BigDecimal" -> (T) new BigDecimal(value.toString());
		default -> JSON.parseObject(value.toString(), rtClazz);
		};
	}

	@Override
	public T handleResponse(final ClassicHttpResponse response) throws IOException {
		final HttpEntity entity = response.getEntity();
		if (response.getCode() >= HttpStatus.SC_REDIRECTION) {
			try {
				String resp = EntityUtils.toString(entity);
				if (resp != null && resp.length() > 0) {
					JSONObject result = JSON.parseObject(resp);
					throw new SysException(result.getString("msg"));
				} else {
					throw new SysException("远程服务异常");
				}
			} catch (ParseException e) {
				log.error("", e);

				throw new SysException("远程服务异常");
			}
		}

		return entity == null ? null : handleEntity(entity);
	}

}