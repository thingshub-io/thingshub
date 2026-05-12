package io.thingshub.logging.logback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.LiteBlockingWaitStrategy;
import com.lmax.disruptor.LiteTimeoutBlockingWaitStrategy;
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;

public class WaitStrategyFactory {

	private static final char PARAM_END_CHAR = '}';
	private static final char PARAM_START_CHAR = '{';
	private static final char PARAM_SEPARATOR_CHAR = ',';

	public static WaitStrategy createWaitStrategyFromString(String waitStrategyType) {
		if (waitStrategyType == null) {
			return null;
		}
		waitStrategyType = waitStrategyType.trim().toLowerCase();
		if (waitStrategyType.isEmpty()) {
			return null;
		}
		if (waitStrategyType.equals("blocking")) {
			return new BlockingWaitStrategy();
		}
		if (waitStrategyType.equals("busyspin")) {
			return new BusySpinWaitStrategy();
		}
		if (waitStrategyType.equals("liteblocking")) {
			return new LiteBlockingWaitStrategy();
		}
		if (waitStrategyType.startsWith("sleeping")) {
			if (waitStrategyType.equals("sleeping")) {
				return new SleepingWaitStrategy();
			} else {
				List<Object> params = parseParams(waitStrategyType, Integer.class, Long.class);
				return new SleepingWaitStrategy((Integer) params.get(0), (Long) params.get(1));
			}
		}
		if (waitStrategyType.equals("yielding")) {
			return new YieldingWaitStrategy();
		}
		if (waitStrategyType.startsWith("phasedbackoff")) {
			List<Object> params = parseParams(waitStrategyType, Long.class, Long.class, TimeUnit.class, WaitStrategy.class);
			return new PhasedBackoffWaitStrategy((Long) params.get(0), (Long) params.get(1), (TimeUnit) params.get(2), (WaitStrategy) params.get(3));
		}
		if (waitStrategyType.startsWith("timeoutblocking")) {
			List<Object> params = parseParams(waitStrategyType, Long.class, TimeUnit.class);
			return new TimeoutBlockingWaitStrategy((Long) params.get(0), (TimeUnit) params.get(1));
		}
		if (waitStrategyType.startsWith("litetimeoutblocking")) {
			List<Object> params = parseParams(waitStrategyType, Long.class, TimeUnit.class);
			return new LiteTimeoutBlockingWaitStrategy((Long) params.get(0), (TimeUnit) params.get(1));
		}

		throw new IllegalArgumentException("Unknown wait strategy type: " + waitStrategyType);

	}

	private static List<Object> parseParams(String waitStrategyType, Class<?>... paramTypes) {
		String paramsString = extractParamsString(waitStrategyType, paramTypes);

		List<Object> params = new ArrayList<Object>(paramTypes.length);

		int startIndex = 0;
		for (int i = 0; i < paramTypes.length && startIndex < paramsString.length(); i++) {
			int endIndex = findParamEndIndex(paramsString, startIndex);
			String paramString = paramsString.substring(startIndex, endIndex).trim();
			startIndex = endIndex + 1;

			if (Integer.class.equals(paramTypes[i])) {
				params.add(Integer.valueOf(paramString));
			} else if (Long.class.equals(paramTypes[i])) {
				params.add(Long.valueOf(paramString));
			} else if (TimeUnit.class.equals(paramTypes[i])) {
				params.add(TimeUnit.valueOf(paramString.toUpperCase()));
			} else if (WaitStrategy.class.equals(paramTypes[i])) {
				params.add(createWaitStrategyFromString(paramString));
			} else {
				throw new IllegalArgumentException("Unknown paramType " + paramTypes[i]);
			}

		}
		if (params.size() != paramTypes.length) {
			throw new IllegalArgumentException(String.format("%d parameters must be provided for waitStrategyType %s. %d were provided.", paramTypes.length,
					waitStrategyType, params.size()));
		}

		return params;
	}

	private static String extractParamsString(String waitStrategyType, Class<?>... paramTypes) {
		int startIndex = waitStrategyType.indexOf(PARAM_START_CHAR);
		if (startIndex == -1) {
			throw new IllegalArgumentException(String.format(
					"%d parameters must be provided for waitStrategyType %s." + " None were provided."
							+ " To provide parameters, add a comma separated value list within curly braces ({}) to the end of the waitStrategyType string.",
					paramTypes.length, waitStrategyType));
		}
		int endIndex = waitStrategyType.lastIndexOf(PARAM_END_CHAR);
		if (endIndex == -1) {
			throw new IllegalArgumentException(String.format("Parameters of %s must end with '}'", waitStrategyType));
		}

		return waitStrategyType.substring(startIndex + 1, endIndex);
	}

	private static int findParamEndIndex(String paramsString, int startIndex) {
		int nestLevel = 0;
		for (int c = startIndex; c < paramsString.length(); c++) {
			char character = paramsString.charAt(c);
			if (character == PARAM_START_CHAR) {
				nestLevel++;
			} else if (character == PARAM_END_CHAR) {
				nestLevel--;
				if (nestLevel < 0) {
					throw new IllegalArgumentException(String.format("Unbalanced '}' at character position %d in %s", c, paramsString));
				}
			} else if (character == PARAM_SEPARATOR_CHAR && nestLevel == 0) {
				return c;
			}
		}
		if (nestLevel != 0) {
			throw new IllegalArgumentException(String.format("Unbalanced '{' in %s", paramsString));
		}
		return paramsString.length();
	}

}
