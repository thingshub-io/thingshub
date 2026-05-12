package io.thingshub.logging;

import java.util.List;

public interface LogService {

	List<LogModel> search(LogSearchParams searchParams);

}
