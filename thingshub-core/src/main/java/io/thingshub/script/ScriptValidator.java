package io.thingshub.script;

import java.util.Map;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScriptValidator {

	private static Map<ScriptType, ScriptEngine> executorsMap;

	private static boolean validate(@NonNull String script, @NonNull ScriptType scriptType) {
		if (executorsMap.isEmpty()) {
			log.error("No script executors found.");
			return false;
		}

		if (scriptType != null && !executorsMap.containsKey(scriptType)) {
			log.error("Specified script type {} was not found.", scriptType);
			return false;
		}

		try {
			executorsMap.get(scriptType).compile(script);
		} catch (Exception e) {
			log.error("Failed to compile {} script. Error: ", scriptType, e);
			return false;
		}

		return true;
	}

	public static boolean validate(Map<ScriptType, String> scripts) {
		for (Map.Entry<ScriptType, String> script : scripts.entrySet()) {
			if (!validate(script.getValue(), script.getKey())) {
				return false;
			}
		}
		return true;
	}
}
