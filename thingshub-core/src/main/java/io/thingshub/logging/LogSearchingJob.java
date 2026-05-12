package io.thingshub.logging;

import java.util.List;

import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.ServiceResource;

public class LogSearchingJob implements IgniteCallable<List<LogModel>> {

	private static final long serialVersionUID = 6423539892610633777L;

	@ServiceResource(serviceName = "logService", proxyInterface = LogService.class)
	private LogService logService;

	private LogSearchParams searchParams;

	public LogSearchingJob(LogSearchParams searchParams) {
		this.searchParams = searchParams;
	}

	public List<LogModel> call() throws Exception {
		return logService.search(searchParams);
	}
}
