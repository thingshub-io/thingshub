package io.thingshub.http;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;

import io.thingshub.http.annotation.RequestMapping;
import lombok.Data;

@Data
public class MethodWrapper {

	private Method method;

	private RequestMapping requestMapping;

	private LinkedHashMap<String, MethodParameter> parameters;

}