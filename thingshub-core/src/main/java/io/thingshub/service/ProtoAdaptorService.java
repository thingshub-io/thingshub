package io.thingshub.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.db.sql.Condition;
import io.thingshub.commons.Page;
import io.thingshub.commons.ServiceException;
import io.thingshub.entity.ProtoAdaptor;
import io.thingshub.entity.ScriptInfo;
import io.thingshub.ioc.Service;
import io.thingshub.script.ScriptEngineFactory;
import io.thingshub.service.base.BaseService;
import io.thingshub.service.base.IdGenerator;
import io.thingshub.service.model.ProtocolAdaptorDetails;
import jakarta.inject.Inject;

/**
 * <p>
 * Protocol Adaptor Management
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class ProtoAdaptorService extends BaseService<Long, ProtoAdaptor> {

	@Inject
	private ScriptService scriptService;

	@Inject
	private ScriptEngineFactory scriptEngineFactory;

	@Inject
	public IdGenerator idGenerator;

	public Page<ProtoAdaptor> queryProtocolAdaptors(Map<String, Object> params, int page, int size) {
		List<Condition> conditions = params.entrySet().stream().map(entry -> {
			// TODO key到col 名称的映射
			if (entry.getKey().equals("identifier")) {
				return new Condition("identifier", entry.getValue());
			}
			return null;
		}).collect(Collectors.toList());
		conditions.add(new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));

		return this.query(conditions, page, size);
	}

	public ProtocolAdaptorDetails getProtocolAdaptorDetails(Long id) {
		ProtoAdaptor protocolAdaptor = this.getByKey(id);
		if (protocolAdaptor == null || protocolAdaptor.getDeletedStatus() == DeletedStatus.DELETED.value()) {
			return null;
		}

		ProtocolAdaptorDetails protocolAdaptorDetails = new ProtocolAdaptorDetails();
		BeanUtil.copyProperties(protocolAdaptor, protocolAdaptorDetails, CopyOptions.create().setIgnoreNullValue(true));

		ScriptInfo theScriptInfo = scriptService.getByKey(protocolAdaptor.getScriptId());
		protocolAdaptorDetails.setScriptLang(theScriptInfo.getLang());
		protocolAdaptorDetails.setScriptContent(theScriptInfo.getContent());

		return protocolAdaptorDetails;
	}

	public Long createProtocolAdaptor(ProtocolAdaptorDetails protocolAdaptorDetails) throws ServiceException {
		List<Condition> queryConditions = Lists.newArrayList( //
				new Condition("identifier", protocolAdaptorDetails.getIdentifier()), //
				new Condition("version", protocolAdaptorDetails.getVersion()), //
				new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));
		if (this.getOne(queryConditions) != null) {
			throw new ServiceException("该版本协议已存在");
		}

		long scriptId = idGenerator.nextId();
		ScriptInfo scriptInfo = new ScriptInfo();
		scriptInfo.setId(scriptId);
		scriptInfo.setName(protocolAdaptorDetails.getIdentifier());
		scriptInfo.setLang(protocolAdaptorDetails.getScriptLang());
		scriptInfo.setContent(protocolAdaptorDetails.getScriptContent());
		scriptInfo.setDeletedStatus(DeletedStatus.NOT_DELETED.value());
		scriptService.save(scriptId, scriptInfo);

		scriptEngineFactory.getScriptEngine(scriptInfo.getLang()).load(scriptInfo.getName(), scriptInfo.getContent());

		long protocolId = idGenerator.nextId();

		ProtoAdaptor protocolAdaptor = new ProtoAdaptor();
		protocolAdaptor.setId(protocolId);
		protocolAdaptor.setIdentifier(protocolAdaptorDetails.getIdentifier());
		protocolAdaptor.setTitle(protocolAdaptorDetails.getTitle());
		protocolAdaptor.setVersion(protocolAdaptorDetails.getVersion());
		protocolAdaptor.setScriptId(scriptInfo.getId());
		protocolAdaptor.setRemark(protocolAdaptorDetails.getRemark());
		protocolAdaptor.setDeletedStatus(DeletedStatus.NOT_DELETED.value());
		protocolAdaptor.setStatus(AvailableStatus.NORMAL.value());
		protocolAdaptor.setCreateTime(new Date());
		protocolAdaptor.setCreateBy(protocolAdaptorDetails.getCreateBy());
		this.save(protocolId, protocolAdaptor);

		return protocolAdaptor.getId();
	}

	public void updateProtocolAdaptor(ProtocolAdaptorDetails protocolAdaptorDetails) throws ServiceException {
		List<Condition> queryConditions = Lists.newArrayList( //
				new Condition("identifier", protocolAdaptorDetails.getIdentifier()), //
				new Condition("version", protocolAdaptorDetails.getVersion()), //
				new Condition("deleted_status", DeletedStatus.NOT_DELETED.value()));
		ProtoAdaptor protocolAdaptor = this.getOne(queryConditions);
		if (protocolAdaptor != null && !protocolAdaptor.getId().equals(protocolAdaptorDetails.getId())) {
			throw new ServiceException("该协议版本已存在");
		}

		ScriptInfo theScriptInfo = scriptService.getByKey(protocolAdaptorDetails.getScriptId());
		theScriptInfo.setLang(protocolAdaptorDetails.getScriptLang());
		theScriptInfo.setContent(protocolAdaptorDetails.getScriptContent());
		scriptService.save(protocolAdaptorDetails.getScriptId(), theScriptInfo);

		scriptEngineFactory.getScriptEngine(theScriptInfo.getLang()).load(theScriptInfo.getName(), theScriptInfo.getContent());

		protocolAdaptor.setIdentifier(protocolAdaptorDetails.getIdentifier());
		protocolAdaptor.setTitle(protocolAdaptorDetails.getTitle());
		protocolAdaptor.setVersion(protocolAdaptorDetails.getVersion());
		protocolAdaptor.setScriptId(theScriptInfo.getId());
		protocolAdaptor.setRemark(protocolAdaptorDetails.getRemark());
		protocolAdaptor.setUpdateTime(new Date());
		protocolAdaptor.setUpdateBy(protocolAdaptorDetails.getUpdateBy());
		this.save(protocolAdaptorDetails.getId(), protocolAdaptor);
	}

	public void remove(Long id) {
		ProtoAdaptor protocolAdaptor = this.getByKey(id);
		if (protocolAdaptor != null) {
			protocolAdaptor.setDeletedStatus(DeletedStatus.DELETED.value());
			this.save(id, protocolAdaptor);

			ScriptInfo theScriptInfo = scriptService.getByKey(protocolAdaptor.getScriptId());
			theScriptInfo.setDeletedStatus(DeletedStatus.DELETED.value());
			scriptService.save(protocolAdaptor.getScriptId(), theScriptInfo);

			scriptEngineFactory.getScriptEngine(theScriptInfo.getLang()).unload(theScriptInfo.getName());
		}
	}

}
