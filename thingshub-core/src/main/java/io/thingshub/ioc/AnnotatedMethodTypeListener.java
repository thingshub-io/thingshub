package io.thingshub.ioc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import io.thingshub.commons.SysException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnnotatedMethodTypeListener<T extends Annotation> implements TypeListener {

	private final Class<T> annotationClazz;

	private final MethodPostProcessor<T> methodPostProcessor;

	public AnnotatedMethodTypeListener(final Class<T> annotationClazz, final MethodPostProcessor<T> methodPostProcessor) {
		this.annotationClazz = annotationClazz;
		this.methodPostProcessor = methodPostProcessor;
	}

	@Override
	public <I> void hear(final TypeLiteral<I> type, final TypeEncounter<I> encounter) {
		final Class<? super I> actualType = type.getRawType();
		if (!IocUtils.isPackageValid(actualType)) {
			return;
		}

		Deque<Method> methodStack = new ArrayDeque<>();
		Class<? super I> investigatingType = actualType;

		while (investigatingType != null && !investigatingType.equals(Object.class)) {
			for (final Method method : investigatingType.getDeclaredMethods()) {
				if (method.isAnnotationPresent(annotationClazz)) {
					methodStack.push(method);
				}
			}

			investigatingType = investigatingType.getSuperclass();
		}

		while (!methodStack.isEmpty()) {
			Method method = methodStack.poll();
			encounter.register(new InjectionListener<I>() {

				@Override
				public void afterInjection(final I injectee) {
					try {
						method.setAccessible(true);
						methodPostProcessor.process(method.getAnnotation(annotationClazz), method, injectee);
					} catch (Exception ex) {
						log.error("", ex);

						throw new SysException(String.format("Failed to process annotation %s on method %s of class %s", annotationClazz.getSimpleName(),
								method.getName(), injectee.getClass().getSimpleName()), ex);
					}
				}

			});
		}
	}
}
