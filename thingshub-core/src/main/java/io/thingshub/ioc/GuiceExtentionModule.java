package io.thingshub.ioc;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public class GuiceExtentionModule extends AbstractModule {

	private Matcher<Object> typeMatcher;

	public GuiceExtentionModule() {
		this(Matchers.any());
	}

	public GuiceExtentionModule(final String pkg) {
		this(new ObjectPackageMatcher<>(pkg));
	}

	public GuiceExtentionModule(final Matcher<Object> typeMatcher) {
		this.typeMatcher = typeMatcher;
	}

	@Override
	protected void configure() {
		final DestroyableManager manager = new DestroyableManager();
		bind(DestroyableManager.class).toInstance(manager);
		Runtime.getRuntime().addShutdownHook(new Thread(manager));

		bindListener(typeMatcher, new GeneralTypeListener<Destroyable>(Destroyable.class, new DestroyableTypeProcessor(manager)));
		bindListener(typeMatcher, new AnnotatedMethodTypeListener<PostConstruct>(PostConstruct.class, new PostConstructAnnotationProcessor()));
		bindListener(typeMatcher, new AnnotatedMethodTypeListener<PreDestroy>(PreDestroy.class, new PreDestroyAnnotationProcessor(manager)));
	}

}