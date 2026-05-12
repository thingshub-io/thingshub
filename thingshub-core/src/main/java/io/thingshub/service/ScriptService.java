package io.thingshub.service;

import java.util.List;

import com.google.common.collect.Lists;

import cn.hutool.db.sql.Condition;
import io.thingshub.entity.ScriptInfo;
import io.thingshub.ioc.Service;
import io.thingshub.script.ScriptEngineFactory;
import io.thingshub.service.base.BaseService;
import io.thingshub.service.base.IdGenerator;
import jakarta.inject.Inject;

/**
 * <p>
 * 脚本信息管理
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class ScriptService extends BaseService<Long, ScriptInfo> {

	@Inject
	private IdGenerator idGenerator;

	@Inject
	private ScriptEngineFactory scriptEngineFactory;

	public void createScript(String name, String lang, String content) {
		long id = idGenerator.nextId();

		ScriptInfo scriptInfo = new ScriptInfo();
		scriptInfo.setId(id);
		scriptInfo.setName(name);
		scriptInfo.setLang(lang);
		scriptInfo.setContent(content);
		scriptInfo.setDeletedStatus(DeletedStatus.NOT_DELETED.value());

		this.save(id, scriptInfo);

		scriptEngineFactory.getScriptEngine(lang).load(name, content);
	}

	public void updateScript(String name, String lang, String content) {
		List<Condition> queryConditions = Lists.newArrayList(new Condition("name", name));
		ScriptInfo scriptInfo = this.getOne(queryConditions);
		if (scriptInfo != null) {
			scriptInfo.setName(name);
			scriptInfo.setLang(lang);
			scriptInfo.setContent(content);

			this.save(scriptInfo.getId(), scriptInfo);

			scriptEngineFactory.getScriptEngine(lang).load(name, content);
		}
	}

	public List<ScriptInfo> getScriptsByLang(String lang) {
		return this.query(Lists.newArrayList(new Condition("lang", lang)));
	}

	public ScriptInfo getScript(String name) {
		return this.getOne(Lists.newArrayList(new Condition("name", name), new Condition("deleted_status", DeletedStatus.NOT_DELETED.value())));
	}

}
