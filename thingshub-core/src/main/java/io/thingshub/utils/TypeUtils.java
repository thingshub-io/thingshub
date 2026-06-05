package io.thingshub.utils;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 泛型处理工具类
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public class TypeUtils {

	public static Object createInstance(Type type) {
		if (type instanceof Class) {
			return createClassInstance((Class<?>) type);
		} else if (type instanceof ParameterizedType) {
			return createParameterizedTypeInstance((ParameterizedType) type);
		} else if (type instanceof GenericArrayType) {
			return createGenericArrayInstance((GenericArrayType) type);
		} else if (type instanceof TypeVariable) {
			return null;
		} else if (type instanceof WildcardType) {
			WildcardType wildcardType = (WildcardType) type;
			Type[] upperBounds = wildcardType.getUpperBounds();
			if (upperBounds.length > 0 && upperBounds[0] != Object.class) {
				return createInstance(upperBounds[0]);
			}

			return null;
		}
		throw new IllegalArgumentException("不支持的Type: " + type);
	}

	private static Object createClassInstance(Class<?> clazz) {
		if (clazz.isPrimitive()) {
			return getPrimitiveDefault(clazz);
		}
		if (clazz.isArray()) {
			return Array.newInstance(clazz.getComponentType(), 0);
		}
		if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
			if (List.class.isAssignableFrom(clazz)) {
				return new ArrayList<>();
			} else if (Set.class.isAssignableFrom(clazz)) {
				return new HashSet<>();
			} else if (Map.class.isAssignableFrom(clazz)) {
				return new HashMap<>();
			} else if (Collection.class.isAssignableFrom(clazz)) {
				return new ArrayList<>();
			}

			throw new UnsupportedOperationException("无法实例化接口/抽象类: " + clazz);
		}

		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object createParameterizedTypeInstance(ParameterizedType paramType) {
		Type rawType = paramType.getRawType();
		Object instance = createInstance(rawType);

		if (instance instanceof Collection) {
			Type[] actualArgs = paramType.getActualTypeArguments();
			if (actualArgs.length == 1) {
				Object element = createInstance(actualArgs[0]);
				if (element != null) {
					((Collection) instance).add(element);
				}
			}
		} else if (instance instanceof Map) {
			Type[] actualArgs = paramType.getActualTypeArguments();
			if (actualArgs.length == 2) {
				Object key = createInstance(actualArgs[0]);
				Object value = createInstance(actualArgs[1]);
				if (key != null && value != null) {
					((Map) instance).put(key, value);
				}
			}
		}

		return instance;
	}

	private static Object createGenericArrayInstance(GenericArrayType genericArrayType) {
		Type componentType = genericArrayType.getGenericComponentType();
		Class<?> componentClass = getRawClass(componentType);
		return Array.newInstance(componentClass, 0);
	}

	private static Class<?> getRawClass(Type type) {
		if (type instanceof Class) {
			return (Class<?>) type;
		} else if (type instanceof ParameterizedType) {
			return (Class<?>) ((ParameterizedType) type).getRawType();
		} else if (type instanceof GenericArrayType) {
			Type componentType = ((GenericArrayType) type).getGenericComponentType();
			return Array.newInstance(getRawClass(componentType), 0).getClass();
		} else if (type instanceof TypeVariable) {
			Type[] bounds = ((TypeVariable<?>) type).getBounds();
			return bounds.length > 0 ? getRawClass(bounds[0]) : Object.class;
		} else if (type instanceof WildcardType) {
			Type[] upperBounds = ((WildcardType) type).getUpperBounds();
			return getRawClass(upperBounds[0]);
		}
		return Object.class;
	}

	private static Object getPrimitiveDefault(Class<?> clazz) {
		if (clazz == boolean.class)
			return false;
		if (clazz == char.class)
			return '\0';
		if (clazz == byte.class)
			return (byte) 0;
		if (clazz == short.class)
			return (short) 0;
		if (clazz == int.class)
			return 0;
		if (clazz == long.class)
			return 0L;
		if (clazz == float.class)
			return 0.0f;
		if (clazz == double.class)
			return 0.0d;
		return null;
	}
}