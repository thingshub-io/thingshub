package io.thingshub.script;

import java.util.List;

/**
 * <p>
 * script engine
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public interface ScriptEngine {

	void load(String name, String script);

	void unload(String name);

	List<String> getScriptNames();

	<T> T invoke(String name, Class<T> clazz, String func, Object... args) throws ScriptException;

	void cleanCache();

	ScriptType scriptType();

	Object compile(String scriptContent) throws Exception;

}
