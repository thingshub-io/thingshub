package io.thingshub.transport.utils;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class TaskTracker {

	private final Set<CompletableFuture<?>> trackedFutures = ConcurrentHashMap.newKeySet();

	private TaskTracker() {
	}

	private static class InstanceHolder {
		private static final TaskTracker instance = new TaskTracker();
	}

	public static TaskTracker getInstance() {
		return InstanceHolder.instance;
	}

	public <T> CompletableFuture<T> track(CompletableFuture<T> trackedFuture) {
		trackedFutures.add(trackedFuture);
		trackedFuture.whenComplete((v, e) -> trackedFutures.remove(trackedFuture));

		return trackedFuture;
	}

	public <T> CompletableFuture<T> track(Supplier<CompletableFuture<T>> futureSupplier) {
		return track(futureSupplier.get());
	}

	public void stop() {
		for (CompletableFuture<?> trackedFuture : trackedFutures) {
			trackedFuture.cancel(true);
		}
	}

	public CompletableFuture<Void> whenComplete(BiConsumer<Void, Throwable> biConsumer) {
		return CompletableFuture.allOf(trackedFutures.stream().toArray(CompletableFuture[]::new)).whenComplete(biConsumer);
	}

	public CompletableFuture<Void> whenCompleteAsync(BiConsumer<Void, Throwable> biConsumer, Executor executor) {
		return CompletableFuture.allOf(trackedFutures.stream().toArray(CompletableFuture[]::new)).whenCompleteAsync(biConsumer, executor);
	}

}