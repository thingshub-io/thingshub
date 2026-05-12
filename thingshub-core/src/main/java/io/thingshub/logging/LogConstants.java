package io.thingshub.logging;

public interface LogConstants {

	String FIELD_HOST = "host";

	String FIELD_TIMESTAMP = "timestamp";

	String FIELD_THREAD = "thread";

	String FIELD_LEVEL = "level";

	String FIELD_LOGGER = "logger";

	String FIELD_METHOD = "method";

	String FIELD_MSG = "msg";

	String FIELD_SORT_ID = "sortid";

	String HIGHLIGHT_PRE_TAG = "<font color='red'>";

	String HIGHLIGHT_POST_TAG = "</font>";

	int FRAGMENT_SIZE = Integer.MAX_VALUE;

	/**
	 * The field DEPLOY_CLUSTER_SINGLETON
	 */
	String DEPLOY_CLUSTER_SINGLETON = "clusterSingleton";

	/**
	 * The field DEPLOY_NODE_SINGLETON
	 */
	String DEPLOY_NODE_SINGLETON = "nodeSingleton";

	String ZLOG_SERVICE_NAME = "zlogService";

	String SYMBOL_AT = "@";

	String SYMBOY_ANY = "*";

	String SYMBOY_COLON = ":";

	String SYMBOY_START_BRACKET = "<";

	String SYMBOY_END_BRACKET = ">";

	String SYMBOY_START_BRACE = "{";

	String SYMBOY_END_BRACE = "}";

	String DEFAULT = "default";

	String CHARACTER_TO = "TO";

	String CHARACTER_AND = "AND";

	String CHARACTER_OR = "OR";
}