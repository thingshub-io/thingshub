package io.thingshub.subscribe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class TopicUtils {

	public static final char DELIMITER_CHAR = '/';
	public static final String DELIMITER = "/";
	public static final String UNORDERED_SHARE = "$share";
	public static final String ORDERED_SHARE = "$oshare";
	public static final char TOPIC_SEPARATOR_CHAR = '/';
	public static final char NULL_CHAR = '\u0000';
	public static final String NULL = "\u0000";
	public static final char SINGLE_WILDCARD_CHAR = '+';
	public static final String SINGLE_WILDCARD = "+";
	public static final char MULTI_WILDCARD_CHAR = '#';
	public static final String MULTI_WILDCARD = "#";
	public static final String PREFIX_UNORDERED_SHARE = UNORDERED_SHARE + DELIMITER_CHAR;
	public static final String PREFIX_ORDERED_SHARE = ORDERED_SHARE + DELIMITER_CHAR;

	public static boolean isValidTopic(String topic, int maxLevelLength, int maxLevel, int maxLength) {
		assert maxLength <= 65535 && maxLevelLength <= maxLength;

		if (topic.isEmpty() || topic.length() > maxLength) {
			// [MQTT-4.7.3-1]
			return false;
		}
		if (topic.startsWith(PREFIX_ORDERED_SHARE) || topic.startsWith(PREFIX_UNORDERED_SHARE)) {
			return false;
		}

		int topicLevelLength = 0;
		int level = 1;
		for (int i = 0; i < topic.length(); i++) {
			if (topic.charAt(i) == TOPIC_SEPARATOR_CHAR) {
				if (++level > maxLevel) {
					return false;
				}
				if (topicLevelLength > maxLevelLength) {
					return false;
				}
				topicLevelLength = 0;
			} else {
				char c = topic.charAt(i);
				if (c == NULL_CHAR || c == SINGLE_WILDCARD_CHAR || c == MULTI_WILDCARD_CHAR) {
					return false;
				}
				topicLevelLength++;
			}
		}

		return topicLevelLength <= maxLevelLength;
	}

	public static boolean isValidTopicFilter(String topicFilter, int maxLevelLength, int maxLevel, int maxLength) {
		if (topicFilter.startsWith(PREFIX_UNORDERED_SHARE)) {
			maxLength += PREFIX_UNORDERED_SHARE.length();
		}
		if (topicFilter.startsWith(PREFIX_ORDERED_SHARE)) {
			maxLength += PREFIX_ORDERED_SHARE.length();
		}
		assert maxLength <= 65535 && maxLevelLength <= maxLength;
		if (topicFilter.isEmpty() || topicFilter.length() > maxLength) {
			// [MQTT-4.7.3-1]
			return false;
		}
		int i = 0;
		int topicLevelLength = 0;
		if (topicFilter.startsWith(PREFIX_ORDERED_SHARE) || topicFilter.startsWith(PREFIX_UNORDERED_SHARE)) {
			// validate share name
			for (i = topicFilter.indexOf(TOPIC_SEPARATOR_CHAR) + 1; i < topicFilter.length(); i++) {
				char c = topicFilter.charAt(i);
				if (c == TOPIC_SEPARATOR_CHAR) {
					break;
				}
				if (c == MULTI_WILDCARD_CHAR || c == SINGLE_WILDCARD_CHAR || c == NULL_CHAR) {
					// [MQTT-4.8.2-2]
					return false;
				}
				topicLevelLength++;
			}
			if (topicLevelLength == 0) {
				// [MQTT-4.8.2-1]
				return false;
			}
			if (i == topicFilter.length()) {
				// [MQTT-4.8.2-2]
				return false;
			}
			topicLevelLength = 0;
			// skip one separator to real topicFilter start pos
			i++;
		}
		int startIdx = i;
		int level = 1;
		for (; i < topicFilter.length(); i++) {
			if (topicFilter.charAt(i) == TOPIC_SEPARATOR_CHAR) {
				if (++level > maxLevel) {
					return false;
				}
				if (topicLevelLength > maxLevelLength) {
					return false;
				}
				topicLevelLength = 0;
			} else {
				char c = topicFilter.charAt(i);
				if (c == NULL_CHAR) {
					// [MQTT-4.7.3-2]
					return false;
				}
				if (c == MULTI_WILDCARD_CHAR) {
					if (i != topicFilter.length() - 1) {
						return false;
					}
					if (i != startIdx && topicFilter.charAt(i - 1) != TOPIC_SEPARATOR_CHAR) {
						return false;
					}
				}
				if (c == SINGLE_WILDCARD_CHAR) {
					if (i == startIdx) {
						if (i != topicFilter.length() - 1 && topicFilter.charAt(i + 1) != TOPIC_SEPARATOR_CHAR) {
							return false;
						}
					} else if (i == topicFilter.length() - 1) {
						if (topicFilter.charAt(i - 1) != TOPIC_SEPARATOR_CHAR) {
							return false;
						}
					} else {
						if (topicFilter.charAt(i - 1) != TOPIC_SEPARATOR_CHAR || topicFilter.charAt(i + 1) != TOPIC_SEPARATOR_CHAR) {
							return false;
						}
					}

				}
				topicLevelLength++;
			}
		}

		if (level > maxLevel) {
			return false;
		}

		return topicLevelLength <= maxLevelLength;
	}

	public static boolean isWildcardTopicFilter(String topicFilter) {
		return topicFilter.indexOf(SINGLE_WILDCARD_CHAR) >= 0 || isMultiWildcardTopicFilter(topicFilter);
	}

	public static boolean isMultiWildcardTopicFilter(String topicFilter) {
		return topicFilter.endsWith("" + MULTI_WILDCARD);
	}

	public static boolean isSharedSubscription(String topicFilter) {
		return isOrderedShared(topicFilter) || isUnorderedShared(topicFilter);
	}

	public static String[] decode(String topicFilter) {
		if (isSharedSubscription(topicFilter)) {
			return new String[] { topicFilter.split(DELIMITER)[1], topicFilter.split(DELIMITER, 3)[2] };
		} else {
			return new String[] { null, topicFilter };
		}
	}

	public static boolean isNormalTopicFilter(String topicFilter) {
		return !isSharedSubscription(topicFilter);
	}

	public static boolean isUnorderedShared(String topicFilter) {
		return topicFilter.startsWith(PREFIX_UNORDERED_SHARE);
	}

	public static boolean isOrderedShared(String topicFilter) {
		return topicFilter.startsWith(PREFIX_ORDERED_SHARE);
	}

	public static String escape(String topicFilter) {
		assert !topicFilter.contains(NULL);

		return topicFilter.replace(DELIMITER, NULL);
	}

	public static String unescape(String topicFilter) {
		return topicFilter.replace(NULL, DELIMITER);
	}

	public static List<String> parse(String tenantId, String topic, boolean isEscaped) {
		List<String> topicLevels = new ArrayList<>();
		topicLevels.add(tenantId);

		return parse(topic, isEscaped, topicLevels);
	}

	// parse a topic or topic filter string into a list of topic levels
	// eg. "/" -> ["",""], "/a" -> ["",a], "a/" -> [a,""]
	public static List<String> parse(String topic, boolean isEscaped) {
		return parse(topic, isEscaped, new ArrayList<>());
	}

	// parse a topic or topic filter string into a list of topic levels
	// eg. "/" -> ["",""], "/a" -> ["",a], "a/" -> [a,""]
	private static List<String> parse(String topic, boolean isEscaped, List<String> topicLevels) {
		char splitter = isEscaped ? NULL_CHAR : DELIMITER_CHAR;
		StringBuilder tl = new StringBuilder();
		for (int i = 0; i < topic.length(); i++) {
			if (topic.charAt(i) == splitter) {
				topicLevels.add(tl.toString());
				tl.delete(0, tl.length());
			} else {
				tl.append(topic.charAt(i));
			}
		}
		topicLevels.add(tl.toString());

		return topicLevels;
	}

	public static String fastJoin(CharSequence delimiter, Iterable<? extends CharSequence> strings) {
		StringBuilder sb = new StringBuilder();
		Iterator<? extends CharSequence> itr = strings.iterator();
		while (itr.hasNext()) {
			sb.append(itr.next());
			if (itr.hasNext()) {
				sb.append(delimiter);
			}
		}

		return sb.toString();
	}

	public static <T> String fastJoin(CharSequence delimiter, Iterable<T> items, Function<T, ? extends CharSequence> toCharSequence) {
		StringBuilder sb = new StringBuilder();
		Iterator<T> itr = items.iterator();
		while (itr.hasNext()) {
			sb.append(toCharSequence.apply(itr.next()));
			if (itr.hasNext()) {
				sb.append(delimiter);
			}
		}
		return sb.toString();
	}

	public static boolean isTopicMatch(String subscribeTopic, String publishTopic) {
		if (subscribeTopic == null || publishTopic == null) {
			return false;
		}

		if (Objects.equals(subscribeTopic, publishTopic)) {
			return true;
		}

		final String[] subLevels = subscribeTopic.split("/");
		final String[] pubLevels = publishTopic.split("/");
		final int subLen = subLevels.length;
		final int pubLen = pubLevels.length;

		int subIndex = 0;
		int pubIndex = 0;
		while (subIndex < subLen && pubIndex < pubLen) {
			String subSeg = subLevels[subIndex];
			String pubSeg = pubLevels[pubIndex];
			if ("#".equals(subSeg)) {
				return (subIndex == subLen - 1);
			}

			if ("+".equals(subSeg)) {
				subIndex++;
				pubIndex++;
				continue;
			}

			if (!subSeg.equals(pubSeg)) {
				return false;
			}

			subIndex++;
			pubIndex++;
		}

		if (pubIndex < pubLen) {
			return false;
		}

		while (subIndex < subLen) {
			String subSeg = subLevels[subIndex];
			if ("#".equals(subSeg)) {
				return (subIndex == subLen - 1);
			}

			if (!"+".equals(subSeg)) {
				return false;
			}

			subIndex++;
		}

		return true;
	}

}