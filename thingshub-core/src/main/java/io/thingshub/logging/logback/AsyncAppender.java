package io.thingshub.logging.logback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceReportingEventHandler;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.Duration;
import io.thingshub.commons.SysException;

public abstract class AsyncAppender<Event extends DeferredProcessingAware, Listener extends AppenderListener<Event>> extends UnsynchronizedAppenderBase<Event> {

	@SuppressWarnings("serial")
	public static class EventFullException extends Exception {
	}

	private static final long SLEEP_TIME_DURING_SHUTDOWN = 50 * 1_000_000L; // 50ms

	protected static final String APPENDER_NAME_FORMAT = "%1$s";

	protected static final String THREAD_INDEX_FORMAT = "%2$d";

	public static final String DEFAULT_THREAD_NAME_FORMAT = "logback-appender-" + APPENDER_NAME_FORMAT + "-" + THREAD_INDEX_FORMAT;

	public static final int DEFAULT_RING_BUFFER_SIZE = 8192;

	public static final ProducerType DEFAULT_PRODUCER_TYPE = ProducerType.MULTI;

//	public static final WaitStrategy DEFAULT_WAIT_STRATEGY = new BlockingWaitStrategy();

	public static final WaitStrategy DEFAULT_WAIT_STRATEGY = new SleepingWaitStrategy();

	public static final int DEFAULT_DROPPED_WARN_FREQUENCY = 1000;

	private static final EventFullException EVENT_FULL_EXCEPTION = new EventFullException();

	static {
		EVENT_FULL_EXCEPTION.setStackTrace(new StackTraceElement[] { new StackTraceElement(AsyncAppender.class.getName(), "append(..)", null, -1) });
	}

	private int ringBufferSize = DEFAULT_RING_BUFFER_SIZE;

	private ProducerType producerType = DEFAULT_PRODUCER_TYPE;

	private WaitStrategy waitStrategy = DEFAULT_WAIT_STRATEGY;

	private String threadNameFormat = DEFAULT_THREAD_NAME_FORMAT;

	private boolean useDaemonThread = true;

	private boolean addDefaultStatusListener = true;

	private int droppedWarnFrequency = DEFAULT_DROPPED_WARN_FREQUENCY;

	private ThreadFactory threadFactory = new WorkerThreadFactory();

	private EventTranslatorOneArg<LogEvent<Event>, Event> eventTranslator = new LogEventTranslator<>();

	private ExceptionHandler<LogEvent<Event>> eventExceptionHandler = new EventExceptionHandler();

	private final AtomicLong consecutiveDroppedCount = new AtomicLong();

	private LogEventFactory<Event> eventFactory = new LogEventFactory<>();

	private final AtomicInteger threadNumber = new AtomicInteger(1);

	/**
	 * event到来时，被通知的Listener
	 */
	protected final List<Listener> listeners = new ArrayList<>();

	/**
	 * -1：禁用超时，一直等待，直到ring buffer有空闲空间 0：不超时，ring buffer空间满了时，立即删除event >0：在该时间内重试
	 */
	private Duration appendTimeout = Duration.buildByMilliseconds(10);

	/**
	 * 当ring buffer满了时，向ring buffer中添加event的时间间隔
	 */
	private Duration appendRetryFrequency = Duration.buildByMilliseconds(5);

	/**
	 * 在程序关闭时花多长时间来等待正在发生的event的处理
	 */
	private Duration shutdownGracePeriod = Duration.buildBySeconds(15);

	/**
	 * 限制同一时间进行重试的线程数量
	 */
	private final ReentrantLock lock = new ReentrantLock();

	private Disruptor<LogEvent<Event>> disruptor;

	protected static class LogEvent<Event> {

		public volatile Event event;

		public void recycle() {
			this.event = null;
		}

	}

	protected static class LogEventFactory<Event> implements EventFactory<LogEvent<Event>> {

		@Override
		public LogEvent<Event> newInstance() {
			return new LogEvent<>();
		}

	}

	private class WorkerThreadFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName(calculateThreadName());
			t.setDaemon(useDaemonThread);
			return t;
		}

	}

	protected static class LogEventTranslator<Event> implements EventTranslatorOneArg<LogEvent<Event>, Event> {

		@Override
		public void translateTo(LogEvent<Event> logEvent, long sequence, Event event) {
			logEvent.event = event;
		}

	}

	private class EventExceptionHandler implements ExceptionHandler<LogEvent<Event>> {

		@Override
		public void handleEventException(Throwable ex, long sequence, LogEvent<Event> event) {
			addError("Unable to process event: " + ex.getMessage(), ex);
		}

		@Override
		public void handleOnStartException(Throwable ex) {
			addError("Unable start disruptor", ex);
		}

		@Override
		public void handleOnShutdownException(Throwable ex) {
			addError("Unable shutdown disruptor", ex);
		}
	}

	/**
	 * 在event被delegated event handler处理之后清除event，以便event能够被垃圾回收
	 */
	private static class CleanEventHandler<Event> implements SequenceReportingEventHandler<LogEvent<Event>>, LifecycleAware {

		private final EventHandler<LogEvent<Event>> delegatedEventHandler;

		private Sequence sequenceCallback;

		CleanEventHandler(EventHandler<LogEvent<Event>> delegatedEventHandler) {
			super();
			this.delegatedEventHandler = delegatedEventHandler;
		}

		@Override
		public void onEvent(LogEvent<Event> event, long sequence, boolean endOfBatch) throws Exception {
			try {
				delegatedEventHandler.onEvent(event, sequence, endOfBatch);
			} finally {
				event.recycle();
				sequenceCallback.set(sequence);
			}
		}

		@Override
		public void onStart() {
			if (delegatedEventHandler instanceof LifecycleAware eventHandler) {
				eventHandler.onStart();
			}
		}

		@Override
		public void onShutdown() {
			if (delegatedEventHandler instanceof LifecycleAware eventHandler) {
				eventHandler.onShutdown();
			}
		}

		@Override
		public void setSequenceCallback(final Sequence sequenceCallback) {
			this.sequenceCallback = sequenceCallback;
		}
	}

	@Override
	public void start() {
		if (addDefaultStatusListener && getStatusManager() != null && getStatusManager().getCopyOfStatusListenerList().isEmpty()) {
			LevelFilteringStatusListener statusListener = new LevelFilteringStatusListener();
			statusListener.setLevelValue(Status.WARN);
			statusListener.setDelegate(new OnConsoleStatusListener());
			statusListener.setContext(getContext());
			statusListener.start();
			getStatusManager().add(statusListener);
		}

		/**
		 * 启动Disruptor
		 */
		this.disruptor = new Disruptor<>(this.eventFactory, this.ringBufferSize, this.threadFactory, this.producerType, this.waitStrategy);
		this.disruptor.setDefaultExceptionHandler(this.eventExceptionHandler);
		this.disruptor.handleEventsWith(new CleanEventHandler<>(getLogEventHandler()));
		this.disruptor.start();

		super.start();
		fireAppenderStarted();
	}

	@Override
	public void stop() {
		if (!super.isStarted()) {
			return;
		}

		super.stop();

		/*
		 * 关闭Disruptor
		 */
		long deadline = getShutdownGracePeriod().getMilliseconds() < 0 ? Long.MAX_VALUE
				: System.currentTimeMillis() + getShutdownGracePeriod().getMilliseconds();
		while (!isRingBufferEmpty() && (System.currentTimeMillis() < deadline)) {
			LockSupport.parkNanos(SLEEP_TIME_DURING_SHUTDOWN);
		}
		this.disruptor.halt();
		if (!isRingBufferEmpty()) {
			addWarn("Some queued events have not been logged due to requested shutdown");
		}

		fireAppenderStopped();
	}

	protected abstract EventHandler<LogEvent<Event>> getLogEventHandler();

	protected boolean isRingBufferEmpty() {
		return this.disruptor.getRingBuffer().hasAvailableCapacity(this.getRingBufferSize());
	}

	@Override
	protected void append(Event event) {
		long startTime = System.nanoTime();

		try {
			prepareForDeferredProcessing(event);
		} catch (SysException e) {
			addWarn("Unable to prepare event for deferred processing. Event output might be missing data.", e);
		}

		try {
			if (enqueue(event)) {
				long consecutiveDropped = this.consecutiveDroppedCount.get();
				if (consecutiveDropped != 0 && this.consecutiveDroppedCount.compareAndSet(consecutiveDropped, 0L)) {
					addWarn("Dropped " + consecutiveDropped + " total events due to ring buffer at max capacity [" + this.ringBufferSize + "]");
				}

				fireEventIndexed(event, System.nanoTime() - startTime);
			} else {
				long consecutiveDropped = this.consecutiveDroppedCount.incrementAndGet();
				if ((consecutiveDropped % this.droppedWarnFrequency) == 1) {
					addWarn("Dropped " + consecutiveDropped + " events (and counting...) due to ring buffer at max capacity [" + this.ringBufferSize + "]");
				}

				fireEventIndexingFailed(event, EVENT_FULL_EXCEPTION);
			}

		} catch (AppenderShutdownException e) {
			addWarn("Appender [" + this.getName() + "] 未启动");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@SuppressWarnings("serial")
	public static class AppenderShutdownException extends Exception {

	}

	private boolean enqueue(Event event) throws AppenderShutdownException, InterruptedException {
		if (this.disruptor.getRingBuffer().tryPublishEvent(this.eventTranslator, event)) {
			return true;
		}

		if (this.appendTimeout.getMilliseconds() == 0) {
			return false;
		}

		long deadline = Long.MAX_VALUE;
		if (this.appendTimeout.getMilliseconds() < 0) {
			lock.lockInterruptibly();
		} else {
			deadline = System.currentTimeMillis() + this.appendTimeout.getMilliseconds();
			if (!lock.tryLock(this.appendTimeout.getMilliseconds(), TimeUnit.MILLISECONDS)) {
				return false;
			}
		}

		long backoff = 1L;
		long backoffLimit = TimeUnit.MILLISECONDS.toNanos(this.appendRetryFrequency.getMilliseconds());

		try {
			do {
				if (!isStarted()) {
					throw new AppenderShutdownException();
				}

				if (deadline <= System.currentTimeMillis()) {
					return false;
				}

				if (Thread.currentThread().isInterrupted()) {
					throw new InterruptedException();
				}

				LockSupport.parkNanos(backoff);
				backoff = Math.min(backoff * 2, backoffLimit);

			} while (!this.disruptor.getRingBuffer().tryPublishEvent(this.eventTranslator, event));

			return true;

		} finally {
			lock.unlock();
		}
	}

	protected void prepareForDeferredProcessing(Event event) {
		event.prepareForDeferredProcessing();
	}

	protected String calculateThreadName() {
		List<Object> threadNameFormatParams = getThreadNameFormatParams();
		return String.format(threadNameFormat, threadNameFormatParams.toArray());
	}

	protected List<Object> getThreadNameFormatParams() {
		return Arrays.<Object>asList(getName(), threadNumber.incrementAndGet());
	}

	protected void fireAppenderStarted() {
		safelyFireEvent(listener -> listener.onAppenderStarted(this));
	}

	protected void fireAppenderStopped() {
		safelyFireEvent(listener -> listener.onAppenderStopped(this));
	}

	protected void fireEventIndexed(Event event, long durationInNanos) {
		safelyFireEvent(listener -> listener.onEventIndexed(this, event, durationInNanos));
	}

	protected void fireEventIndexingFailed(Event event, Throwable reason) {
		safelyFireEvent(listener -> listener.onEventIndexingFailed(this, event, reason));
	}

	protected void safelyFireEvent(Consumer<Listener> callback) {
		for (Listener listener : listeners) {
			try {
				callback.accept(listener);
			} catch (Exception e) {
				addError("Failed to invoke listener " + listener, e);
			}
		}
	}

	protected void setEventFactory(LogEventFactory<Event> eventFactory) {
		this.eventFactory = eventFactory;
	}

	protected EventTranslatorOneArg<LogEvent<Event>, Event> getEventTranslator() {
		return eventTranslator;
	}

	protected void setEventTranslator(EventTranslatorOneArg<LogEvent<Event>, Event> eventTranslator) {
		this.eventTranslator = eventTranslator;
	}

	protected Disruptor<LogEvent<Event>> getDisruptor() {
		return disruptor;
	}

	public String getThreadNameFormat() {
		return threadNameFormat;
	}

	public void setThreadNameFormat(String threadNameFormat) {
		this.threadNameFormat = Objects.requireNonNull(threadNameFormat);
	}

	public int getRingBufferSize() {
		return ringBufferSize;
	}

	public void setRingBufferSize(int ringBufferSize) {
		if (ringBufferSize <= 0 || !isPowerOfTwo(ringBufferSize)) {
			throw new IllegalArgumentException("ringBufferSize must be a positive power of 2");
		}
		this.ringBufferSize = ringBufferSize;
	}

	public ProducerType getProducerType() {
		return producerType;
	}

	public WaitStrategy getWaitStrategy() {
		return waitStrategy;
	}

	public void setWaitStrategy(WaitStrategy waitStrategy) {
		this.waitStrategy = Objects.requireNonNull(waitStrategy);
	}

	public void setWaitStrategyType(String waitStrategyType) {
		setWaitStrategy(WaitStrategyFactory.createWaitStrategyFromString(waitStrategyType));
	}

	public Duration getAppendRetryFrequency() {
		return appendRetryFrequency;
	}

	public void setAppendRetryFrequency(Duration appendRetryFrequency) {
		if (Objects.requireNonNull(appendRetryFrequency).getMilliseconds() <= 0) {
			throw new IllegalArgumentException("appendRetryFrequency must be > 0");
		}
		this.appendRetryFrequency = appendRetryFrequency;
	}

	public Duration getAppendTimeout() {
		return appendTimeout;
	}

	public void setAppendTimeout(Duration appendTimeout) {
		this.appendTimeout = Objects.requireNonNull(appendTimeout);
	}

	public void setShutdownGracePeriod(Duration shutdownGracePeriod) {
		this.shutdownGracePeriod = Objects.requireNonNull(shutdownGracePeriod);
	}

	public Duration getShutdownGracePeriod() {
		return shutdownGracePeriod;
	}

	public ThreadFactory getThreadFactory() {
		return threadFactory;
	}

	public void setThreadFactory(ThreadFactory threadFactory) {
		this.threadFactory = Objects.requireNonNull(threadFactory);
	}

	public int getDroppedWarnFrequency() {
		return droppedWarnFrequency;
	}

	public void setDroppedWarnFrequency(int droppedWarnFrequency) {
		this.droppedWarnFrequency = droppedWarnFrequency;
	}

	public boolean isDaemon() {
		return useDaemonThread;
	}

	public void setDaemon(boolean useDaemonThread) {
		this.useDaemonThread = useDaemonThread;
	}

	public void addListener(Listener listener) {
		this.listeners.add(Objects.requireNonNull(listener));
	}

	public void removeListener(Listener listener) {
		this.listeners.remove(listener);
	}

	public boolean isAddDefaultStatusListener() {
		return addDefaultStatusListener;
	}

	public void setAddDefaultStatusListener(boolean addDefaultStatusListener) {
		this.addDefaultStatusListener = addDefaultStatusListener;
	}

	private static boolean isPowerOfTwo(int x) {
		return x != 0 && ((x & (x - 1)) == 0);
	}
}
