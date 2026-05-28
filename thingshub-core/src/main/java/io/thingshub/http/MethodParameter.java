package io.thingshub.http;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import lombok.Data;

@Data
public class MethodParameter {

	private String requestParamName;

	private String name;

	private Type type;

//	private Class<?> clazz;

	private Field[] parameterObjectFields;

	private Class<?>[] validationGroups;

	private boolean needValidation;
}