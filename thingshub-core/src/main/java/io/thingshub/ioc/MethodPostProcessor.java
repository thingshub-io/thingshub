package io.thingshub.ioc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import io.thingshub.commons.SysException;

public interface MethodPostProcessor<T extends Annotation> {

	default void checkNoParams(final Method method) {
		if (method.getParameterTypes().length > 0) {
			throw new SysException("Method without parameters required");
		}
	};

	void process(T annotation, Method method, Object instance) throws Exception;
}
