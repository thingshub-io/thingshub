package io.thingshub.ioc;

import java.lang.reflect.Method;

import jakarta.annotation.PostConstruct;

public class PostConstructAnnotationProcessor implements MethodPostProcessor<PostConstruct> {

	@Override
	public void process(final PostConstruct annotation, final Method method, final Object instance) throws Exception {
		checkNoParams(method);

		method.invoke(instance);
	}

}
