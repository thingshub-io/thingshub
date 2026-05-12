package io.thingshub.ioc;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DestroyableManager implements Runnable {

	private final List<Destroyable> destroyListeners = new ArrayList<>();

	public void register(final Destroyable destroyable) {
		destroyListeners.add(destroyable);
	}

	public void destroy() {
		synchronized (this) {
			for (Destroyable destroyable : destroyListeners) {
				try {
					destroyable.preDestroy();
				} catch (Exception ex) {
					log.error("Failed to properly destroy bean", ex);
				}
			}
			destroyListeners.clear();
		}
	}

	@Override
	public void run() {
		destroy();
	}
}
