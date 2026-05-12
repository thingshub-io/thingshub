package io.thingshub;

import java.util.List;
import java.util.Set;

import io.thingshub.entity.ScriptInfo;
import io.thingshub.ioc.ApplicationRunner;
import io.thingshub.ioc.Component;
import io.thingshub.script.ScriptEngine;
import io.thingshub.service.ScriptService;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Load script and precompile after application initialization
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Component
@Slf4j
public class ScriptLoader implements ApplicationRunner {

	@Inject
	private ScriptService scriptService;

	@Inject
	public Set<ScriptEngine> scriptEngines;

	@Override
	public void run() throws Exception {
		scriptEngines.forEach(se -> {
			String lang = se.scriptType().lang();

			List<ScriptInfo> scripts = scriptService.getScriptsByLang(lang);
			if (scripts != null) {
				scripts.forEach(s -> {
					se.load(s.getName(), s.getContent());
				});
			}
		});

		log.info("Load script and precompile");
	}

}
