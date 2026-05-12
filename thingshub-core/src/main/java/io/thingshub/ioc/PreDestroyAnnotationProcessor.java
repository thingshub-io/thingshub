package io.thingshub.ioc;

import java.lang.reflect.Method;

import jakarta.annotation.PreDestroy;

public class PreDestroyAnnotationProcessor implements MethodPostProcessor<PreDestroy> {

	private final DestroyableManager manager;

	public PreDestroyAnnotationProcessor(final DestroyableManager manager) {
		this.manager = manager;
	}

	@Override
	public void process(final PreDestroy annotation, final Method method, final Object instance) throws Exception {
		checkNoParams(method);

		manager.register(new AnnotatedMethodDestroyable(method, instance));
	}
}
