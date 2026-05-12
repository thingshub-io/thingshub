package io.thingshub.ioc;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import io.thingshub.commons.SysException;

public class GeneralTypeListener<T> implements TypeListener {

	private final Class<T> typeClazz;

	private final TypePostProcessor<T> typePostProcessor;

	public GeneralTypeListener(final Class<T> typeClazz, final TypePostProcessor<T> typePostProcessor) {
		this.typeClazz = typeClazz;
		this.typePostProcessor = typePostProcessor;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <I> void hear(final TypeLiteral<I> type, final TypeEncounter<I> encounter) {
		final Class<? super I> actualType = type.getRawType();
		if (!IocUtils.isPackageValid(actualType)) {
			return;
		}

		if (typeClazz.isAssignableFrom(actualType)) {
			encounter.register(new InjectionListener<I>() {
				@Override
				public void afterInjection(final I injectee) {
					try {
						typePostProcessor.process((T) injectee);
					} catch (Exception ex) {
						throw new SysException(
								String.format("Failed to process type %s of class %s", typeClazz.getSimpleName(), injectee.getClass().getSimpleName()), ex);
					}
				}
			});
		}
	}
}
