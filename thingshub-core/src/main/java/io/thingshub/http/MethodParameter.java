package io.thingshub.http;

import java.lang.reflect.Field;

import lombok.Data;

@Data
public class MethodParameter {

	private String requestParamName;

	private String name;

	private Class<?> type;

	private Field[] parameterObjectFields;

	private Class<?>[] validationGroups;

	private boolean needValidation;
}