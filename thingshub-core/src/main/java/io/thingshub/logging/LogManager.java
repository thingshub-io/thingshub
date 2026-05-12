package io.thingshub.logging;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ignite.Ignite;
import org.apache.ignite.cluster.ClusterGroupEmptyException;

import io.thingshub.ioc.Component;
import jakarta.inject.Inject;

@Component
public class LogManager {

	@Inject
	private Ignite ignite;

	public List<LogModel> search(LogSearchParams params) {
		try {
			Collection<List<LogModel>> result = ignite.compute().broadcast(new LogSearchingJob(params));
			if (result == null) {
				return null;
			}

			List<LogModel> mergedBizlogs = result.stream().filter(logs -> logs != null && !logs.isEmpty()).flatMap(item -> item.stream())
					.sorted(Comparator.comparingDouble(LogModel::getScore)).collect(Collectors.toList());

			int from = (params.getPage() - 1) * params.getSize();
			int to = mergedBizlogs.size() >= params.getSize() ? params.getPage() * params.getSize() : mergedBizlogs.size();
			return mergedBizlogs.subList(from, to);
		} catch (ClusterGroupEmptyException e) {
			throw new LogException(e);
		}
	}

}
