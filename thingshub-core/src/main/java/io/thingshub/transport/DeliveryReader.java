package io.thingshub.transport;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import org.reactivestreams.Subscription;

import io.thingshub.ioc.Component;
import io.thingshub.service.InboxService;
import io.thingshub.service.model.Delivery;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitResult;
import reactor.core.scheduler.Schedulers;

/**
 * <p>
 * Fetch offline deliveries or read real-time deliveries from message inbox
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Component
@Slf4j
public class DeliveryReader {

	@Inject
	private InboxService inboxService;

	private final Map<String, Pipeline> messagePipelines = new ConcurrentHashMap<>();

	@PostConstruct
	public void init() {
		inboxService.listen((eventType, inbox) -> {
			switch (eventType) {
			case CREATED -> {
				Delivery delivery = Delivery.builder().id(inbox.getId()).receiverId(inbox.getReceiverId()).recProps(inbox.getRecProps()).senderId(inbox.getSenderId())
						.sendProps(inbox.getSendProps()).topic(inbox.getTopic()).payload(inbox.getPayload()).deliverySource(DeliverySource.of(inbox.getDeliverySource()))
						.deliverTime(inbox.getDeliverTime()).build();
				Pipeline ppl = messagePipelines.get(delivery.receiverId());
				if (ppl != null) {
					ppl.attach(delivery);
				}
			}
			case UPDATED -> {
				// TODO
			}
			default -> {
			}
			}
		});
	}

	public void start(String clientId, int receiveMaximum, Consumer<Delivery> handler) {
		Pipeline ppl = new Pipeline(clientId, receiveMaximum, handler);
		messagePipelines.put(clientId, ppl);
	}

	public void ackDelivery(String clientId, long deliveryId) {
		inboxService.ackedDelivery(deliveryId);

		Pipeline ppl = messagePipelines.get(clientId);
		if (ppl != null) {
			ppl.hookOnAcked(deliveryId);
		}
	}

	public void stop(String clientId) {
		Pipeline ppl = messagePipelines.remove(clientId);
		if (ppl != null) {
			ppl.terminate();
		}
	}

	private class Pipeline {

		private final Sinks.Many<Delivery> deliverySink;

		private final Disposable deliveryFetcher;

		private final AtomicBoolean fetching = new AtomicBoolean();

		private final AtomicBoolean noMore = new AtomicBoolean(false);

		private final LongAdder pendings = new LongAdder();

		private final String clientId;

		private final int receiveMaximum;

		private final Date deliverEndTime = new Date();

		private final BaseSubscriber<Delivery> subscriber;

		public Pipeline(String clientId, int receiveMaximum, Consumer<Delivery> handler) {
			this.clientId = clientId;
			this.receiveMaximum = receiveMaximum;
			this.deliverySink = Sinks.many().unicast().onBackpressureBuffer();
			this.subscriber = new BaseSubscriber<>() {

				@Override
				protected void hookOnSubscribe(Subscription subscription) {
					request(256);
				}

				@Override
				protected void hookOnNext(Delivery delivery) {
					handler.accept(delivery);
				}

				@Override
				protected void hookOnError(Throwable t) {
					log.error("", t);
				}

				@Override
				protected void hookOnComplete() {
				}
			};

			deliverySink.asFlux().doOnError(t -> log.error("", t)).onErrorResume(t -> Mono.empty()).publishOn(Schedulers.boundedElastic()).subscribeWith(this.subscriber);
			deliveryFetcher = Flux.interval(Duration.ofMillis(100)) //
					.filter(s -> shouldFetchMore())//
					.takeUntil(c -> noMore.get()) //
					.concatMap(tick -> loadDeliveries())//
					.doOnNext(item -> {
						EmitResult result = deliverySink.tryEmitNext(item);
						if (result.isFailure()) {
							log.warn("Emitting delivery failed: {}, {}", result.name(), item);
						} else {
							pendings.increment();
						}
					}).subscribeOn(Schedulers.boundedElastic()).subscribe(result -> {
					}, error -> log.error("fetch error: ", error));
		}

		public void attach(Delivery delivery) {
			EmitResult result = deliverySink.tryEmitNext(delivery);
			if (result.isFailure()) {
				log.warn("Attaching delivery failed: {}, {}", result.name(), delivery.id());
			} else {
				pendings.increment();
			}
		}

		private boolean shouldFetchMore() {
			if (fetching.get()) {
				return false;
			}

			if (pendings.intValue() > receiveMaximum) {
				return false;
			}

			return true;
		}

		public Flux<Delivery> loadDeliveries() {
			if (noMore.get()) {
				return Flux.empty();
			} else if (fetching.compareAndSet(false, true)) {
				List<Delivery> deliveries = inboxService.fetchOfflineDeliveries(clientId, deliverEndTime, 1024);
				if (deliveries == null || deliveries.size() == 0) {
					noMore.set(true);
					fetching.set(false);

					return Flux.empty();
				} else {
					fetching.set(false);
					return Flux.fromIterable(deliveries);
				}
			} else {
				return Flux.empty();
			}
		}

		public void hookOnAcked(long deliveryId) {
			pendings.decrement();
			subscriber.request(1);
		}

		public void terminate() {
			EmitResult emitResult = deliverySink.tryEmitComplete();
			if (emitResult != EmitResult.OK) {
				log.error("Delivery sink complete error: ", emitResult);
			}

			if (!deliveryFetcher.isDisposed()) {
				deliveryFetcher.dispose();
			}
		}
	};
}
