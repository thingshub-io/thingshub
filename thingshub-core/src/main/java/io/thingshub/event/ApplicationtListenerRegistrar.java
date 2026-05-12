package io.thingshub.event;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import io.thingshub.Broker;
import io.thingshub.ioc.ApplicationRunner;
import io.thingshub.ioc.Component;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Register application listener after application initialization
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Component
@Slf4j
public class ApplicationtListenerRegistrar implements ApplicationRunner {

	@Inject
	private ApplicationEventManager eventManager;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void run() throws Exception {
		Configuration reflectionsCfg = new ConfigurationBuilder().forPackages(Broker.class.getPackageName()).setParallel(true).setScanners(Scanners.SubTypes);

		Set<Class<? extends ApplicationListener>> listenerTypes = new Reflections(reflectionsCfg).getSubTypesOf(ApplicationListener.class);
		if (listenerTypes != null && listenerTypes.size() > 0) {
			for (Class<? extends ApplicationListener> listenerClazz : listenerTypes) {
				ApplicationListener listenerBean = Broker.getBean(listenerClazz);
				Type[] types = listenerClazz.getGenericInterfaces();
				Type[] typeArgs = ((ParameterizedType) types[0]).getActualTypeArguments();

				eventManager.addListener((Class<? extends ApplicationEvent>) typeArgs[0], listenerBean);
			}

			log.info("register application listener");
		}
	}

}
