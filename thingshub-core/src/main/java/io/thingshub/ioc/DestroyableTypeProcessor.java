package io.thingshub.ioc;

public class DestroyableTypeProcessor implements TypePostProcessor<Destroyable> {

	private final DestroyableManager manager;

	public DestroyableTypeProcessor(final DestroyableManager manager) {
		this.manager = manager;
	}

	@Override
	public void process(final Destroyable instance) {
		manager.register(instance);
	}
}
