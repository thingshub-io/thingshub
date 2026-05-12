package io.thingshub.logging.logback;

import java.io.File;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.TextField;

import com.alibaba.fastjson2.JSON;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LifecycleAware;

import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import io.thingshub.logging.LogConstants;
import io.thingshub.logging.LogException;
import io.thingshub.logging.LogModel;
import io.thingshub.logging.LogWriter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractLuceneAppender<Event extends DeferredProcessingAware, Listener extends AppenderListener<Event>>
		extends AsyncAppender<Event, Listener> {

	private static final int ONEDAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;

	private static final int DEFAULT_EXPIRATION_DAYS = 30;

	protected static String INDEX_DIR;

	// =================== Appender配置 ===================

	private String dir;// 索引文件目录

	private int expiration = DEFAULT_EXPIRATION_DAYS; // 索引过期时间，单位：天

	private Encoder<Event> encoder;

	// ===================================================

	private LogWriter sysLogWriter;

	private ScheduledThreadPoolExecutor executorService;

	private class LogEventHandler implements EventHandler<LogEvent<Event>>, LifecycleAware {

		@Override
		public void onEvent(LogEvent<Event> logEvent, long sequence, boolean endOfBatch) throws Exception {
			byte[] data;
			try {
				data = encoder.encode(logEvent.event);
			} catch (Exception e) {
				fireEventIndexingFailed(logEvent.event, e.getCause());

				throw e;
			}

			if (data != null) {
				try {
					long startNanoTime = System.nanoTime();

					LogModel bizLog = JSON.parseObject(new String(data), LogModel.class);
					String host = InetAddress.getLocalHost().getHostAddress();

					Document doc = new Document();
					doc.add(new TextField(LogConstants.FIELD_HOST, host, TextField.Store.YES));
					doc.add(new NumericDocValuesField(LogConstants.FIELD_TIMESTAMP, bizLog.getTimestamp()));
					doc.add(new TextField(LogConstants.FIELD_THREAD, bizLog.getThread(), TextField.Store.YES));
					doc.add(new TextField(LogConstants.FIELD_LEVEL, bizLog.getLevel(), TextField.Store.YES));
					doc.add(new TextField(LogConstants.FIELD_LOGGER, bizLog.getLogger(), TextField.Store.YES));
					doc.add(new TextField(LogConstants.FIELD_MSG, bizLog.getMsg(), TextField.Store.YES));
					doc.add(new NumericDocValuesField(LogConstants.FIELD_SORT_ID, bizLog.getTimestamp()));

					sysLogWriter.addDocument(doc);
					sysLogWriter.commit();

					long endNanoTime = System.nanoTime();
					fireEventIndexed(logEvent.event, endNanoTime - startNanoTime);
				} catch (Throwable e) {
					log.error("", e);
				}
			}
		}

		@Override
		public void onStart() {
		}

		@Override
		public void onShutdown() {
		}

	}

	public AbstractLuceneAppender() {
		super();
		setThreadNameFormat(DEFAULT_THREAD_NAME_FORMAT);

		this.executorService = new ScheduledThreadPoolExecutor(2, getThreadFactory());
		this.executorService.setRemoveOnCancelPolicy(true);
	}

	private LogWriter buildSysLogWriter() {
		if (new File(this.dir).isFile()) {
			throw new LogException("日志存储路径必须是目录，'" + dir + "'是一个文件");
		}

		return LogWriter.getInstance(new CJKAnalyzer(), dir);
	}

	private void registerCleanTimer() {
		Calendar calendar = Calendar.getInstance();
		long curMillis = calendar.getTimeInMillis();
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		long difMinutes = (calendar.getTimeInMillis() - curMillis) / (1000 * 60);

		long expiryTime = this.expiration * ONEDAY_IN_MILLISECONDS;

		executorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if (null != sysLogWriter) {
					Long start = System.currentTimeMillis() - expiryTime;
					Long end = System.currentTimeMillis();
//					NumericRangeQuery<Long> rangeQuery = NumericRangeQuery.newLongRange("time", start, end, true, true);
//					zlogWriter.deleteDocuments(rangeQuery);
//					zlogWriter.commit();
				}
			}
		}, difMinutes, 1440, TimeUnit.MINUTES);
	}

	@Override
	protected EventHandler<LogEvent<Event>> getLogEventHandler() {
		return new LogEventHandler();
	}

	@Override
	public synchronized void start() {
		if (isStarted()) {
			return;
		}

		int errorCount = 0;
		if (dir == null || dir.isBlank()) {
			errorCount++;
			addError("未配置日志存储目录");
		}
		if (encoder == null) {
			errorCount++;
			addError("未配置encoder");
		}

		assert expiration >= 1;
		assert expiration <= 180;

		if (errorCount == 0) {
			encoder.setContext(getContext());
			if (!encoder.isStarted()) {
				encoder.start();
			}

			super.start();
		}

		sysLogWriter = buildSysLogWriter();
		registerCleanTimer();
	}

	@Override
	public synchronized void stop() {
		if (!isStarted()) {
			return;
		}

		super.stop();
		executorService.shutdown();

		try {
			if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
				addWarn("Some queued events have not been logged due to requested shutdown");
			}

			sysLogWriter.close();
			encoder.stop();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			addWarn("Some queued events have not been logged due to requested shutdown", e);
		}
	}

	public Encoder<Event> getEncoder() {
		return encoder;
	}

	public void setEncoder(Encoder<Event> encoder) {
		this.encoder = encoder;
	}

	public void setDir(String dir) {
		this.dir = dir;
		INDEX_DIR = dir;
	}

	public String getDir() {
		return this.dir;
	}

	public void setExpiration(int expiration) {
		this.expiration = expiration;
	}

	public int getExpiration() {
		return this.expiration;
	}

	protected void updateCurrentThreadName() {
		Thread.currentThread().setName(calculateThreadName());
	}

}