package io.thingshub;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

import io.thingshub.commons.utils.ServiceLoaderHelper;
import io.thingshub.ioc.Component;
import io.thingshub.ioc.Service;
import io.thingshub.plugin.Registry;
import io.thingshub.script.ScriptEngine;
import io.thingshub.transport.Processor;
import io.thingshub.transport.Server;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Core Module
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class CoreModule extends AbstractModule {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void configure() {
		Configuration configuration = new ConfigurationBuilder().forPackages(Broker.class.getPackageName()).setParallel(true);
		Reflections reflections = new Reflections(configuration);

		Set<Class<?>> serviceClazzs = reflections.get(Scanners.TypesAnnotated.with(Service.class).asClass());
		if (serviceClazzs != null) {
			for (Class<?> serviceClazz : serviceClazzs) {
				bind(serviceClazz).in(Singleton.class);
			}
		}

		Set<Class<?>> componentClazzs = reflections.get(Scanners.TypesAnnotated.with(Component.class).asClass());
		if (componentClazzs != null) {
			List<Class<?>> metaAnnotations = componentClazzs.stream().peek(c -> {
				if (!c.isAnnotation()) {
					bind(c).in(Singleton.class);
				}
			}).filter(Class::isAnnotation).toList();

			if (metaAnnotations != null) {
				for (Class<?> metaAnnotation : metaAnnotations) {
					Set<Class<?>> clazzsWithMetaAnnotation = reflections
							.get(Scanners.TypesAnnotated.with((Class<? extends Annotation>) metaAnnotation).asClass());
					if (clazzsWithMetaAnnotation != null) {
						for (Class<?> clazzWithMetaAnnotation : clazzsWithMetaAnnotation) {
							bind(clazzWithMetaAnnotation).in(Singleton.class);
						}
					}
				}
			}
		}

		Set<Class<? extends ScriptEngine>> seClazzs = reflections.getSubTypesOf(ScriptEngine.class);
		if (seClazzs != null) {
			Multibinder<ScriptEngine> scriptEngineBinder = Multibinder.newSetBinder(binder(), ScriptEngine.class);
			seClazzs.forEach(seClazz -> {
				try {
					if (!seClazz.isInterface() && !Modifier.isAbstract(seClazz.getModifiers())) {
						scriptEngineBinder.addBinding().toInstance(seClazz.getDeclaredConstructor().newInstance());
					}
				} catch (Exception e) {
					log.error("", e);
				}
			});
		}

		Multibinder<Server> serverBinder = Multibinder.newSetBinder(binder(), Server.class);
		Set<Class<? extends Server>> serverClazzs = reflections.getSubTypesOf(Server.class);
		if (serverClazzs != null) {
			serverClazzs.forEach(serverClazz -> {
				try {
					if (!serverClazz.isInterface() && !Modifier.isAbstract(serverClazz.getModifiers())) {
						serverBinder.addBinding().toInstance(serverClazz.getDeclaredConstructor().newInstance());
					}
				} catch (Exception e) {
					log.error("", e);
				}
			});
		}
		// Registries in extensions
		ServiceLoaderHelper.findAll(Server.class).forEach(server -> {
			serverBinder.addBinding().toInstance(server);
		});

		Multibinder<Processor> processorBinder = Multibinder.newSetBinder(binder(), Processor.class);
		Set<Class<? extends Processor>> processorClazzs = reflections.getSubTypesOf(Processor.class);
		if (processorClazzs != null) {
			processorClazzs.forEach(processorClazz -> {
				try {
					if (!processorClazz.isInterface() && !Modifier.isAbstract(processorClazz.getModifiers())) {
						processorBinder.addBinding().toInstance(processorClazz.getDeclaredConstructor().newInstance());
					}
				} catch (Exception e) {
					log.error("", e);
				}
			});
		}
		// Registries in extensions
		ServiceLoaderHelper.findAll(Processor.class).forEach(processor -> {
			processorBinder.addBinding().toInstance(processor);
		});

		Multibinder<Registry> registryBinder = Multibinder.newSetBinder(binder(), Registry.class);
		Set<Class<? extends Registry>> registryClazzs = reflections.getSubTypesOf(Registry.class);
		if (registryClazzs != null) {
			registryClazzs.forEach(registryClazz -> {
				try {
					if (!registryClazz.isInterface() && !Modifier.isAbstract(registryClazz.getModifiers())) {
						registryBinder.addBinding().toInstance(registryClazz.getDeclaredConstructor().newInstance());
					}
				} catch (Exception e) {
					log.error("", e);
				}
			});
		}

	}

}