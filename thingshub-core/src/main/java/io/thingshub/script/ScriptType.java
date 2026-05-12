package io.thingshub.script;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 脚本类别
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public enum ScriptType {

	JS("javascript", "js"), PYTHON("python", "python");

	@Accessors(fluent = true)
	@Getter
	private final String lang;

	@Accessors(fluent = true)
	@Getter
	private final String title;

	ScriptType(String lang, String title) {
		this.lang = lang;
		this.title = title;
	}

	public static ScriptType of(String lang) {
		for (ScriptType e : ScriptType.values()) {
			if (e.lang().equals(lang)) {
				return e;
			}
		}
		return null;
	}

}
