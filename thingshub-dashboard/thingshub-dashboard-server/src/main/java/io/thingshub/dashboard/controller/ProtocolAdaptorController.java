package io.thingshub.dashboard.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import io.thingshub.commons.IdParam;
import io.thingshub.commons.ItemOption;
import io.thingshub.commons.LongId;
import io.thingshub.commons.Page;
import io.thingshub.commons.ServiceException;
import io.thingshub.dashboard.params.ProtocolAdaptorRequestParams.ProtocolAdaptorFormParams;
import io.thingshub.dashboard.params.ProtocolAdaptorRequestParams.QueryProtocolAdaptorParams;
import io.thingshub.dashboard.server.UserContextHolder;
import io.thingshub.dashboard.server.UserInfo;
import io.thingshub.entity.ProtoAdaptor;
import io.thingshub.http.HttpMethod;
import io.thingshub.http.annotation.Controller;
import io.thingshub.http.annotation.RequestBody;
import io.thingshub.http.annotation.RequestMapping;
import io.thingshub.http.annotation.RequestParam;
import io.thingshub.script.ScriptType;
import io.thingshub.service.ProtoAdaptorService;
import io.thingshub.service.model.ProtocolAdaptorDetails;
import jakarta.inject.Inject;

/**
 * <p>
 * 协议适配器管理接口
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Controller
public class ProtocolAdaptorController {

	@Inject
	private ProtoAdaptorService protocolAdaptorService;

	@RequestMapping(path = "/protocol-adaptor/script-types")
	public List<ItemOption> listScriptTypes() {
		List<ItemOption> options = Lists.newArrayList();

		for (ScriptType scriptType : ScriptType.values()) {
			options.add(new ItemOption(scriptType.lang(), scriptType.title()));
		}

		return options;
	}

	@RequestMapping(path = "/protocol-adaptor/query")
	public Page<ProtoAdaptor> queryProtocolAdaptors(QueryProtocolAdaptorParams params) {
		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("identifier", params.getIdentifier());

		return protocolAdaptorService.queryProtocolAdaptors(queryParams, params.getPage(), params.getSize());
	}

	@RequestMapping(path = "/protocol-adaptor/details")
	public ProtocolAdaptorDetails getProtocolAdaptorDetails(@RequestParam Long id) {
		return protocolAdaptorService.getProtocolAdaptorDetails(id);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/protocol-adaptor/save")
	public LongId saveProtocolAdaptor(@RequestBody ProtocolAdaptorFormParams params) throws ServiceException {
		UserInfo userInfo = UserContextHolder.getCurrentUser();

		ProtocolAdaptorDetails protocolAdaptorDetails = new ProtocolAdaptorDetails();
		protocolAdaptorDetails.setIdentifier(params.getIdentifier());
		protocolAdaptorDetails.setTitle(params.getProtocolTitle());
		protocolAdaptorDetails.setVersion(params.getProtocolVersion());
		protocolAdaptorDetails.setScriptLang(params.getScriptLang());
		protocolAdaptorDetails.setScriptContent(params.getScriptContent());
		protocolAdaptorDetails.setRemark(params.getRemark());

		Long adaptorId = params.getId();
		if (adaptorId == null) {
			protocolAdaptorDetails.setCreateBy(userInfo.getName());
			adaptorId = protocolAdaptorService.createProtocolAdaptor(protocolAdaptorDetails);
		} else {
			if (params.getScriptId() == null) {
				throw new ServiceException("协议脚本ID不能为空");
			}

			protocolAdaptorDetails.setId(adaptorId);
			protocolAdaptorDetails.setScriptId(params.getScriptId());
			protocolAdaptorDetails.setUpdateBy(userInfo.getName());
			protocolAdaptorService.updateProtocolAdaptor(protocolAdaptorDetails);
		}

		return new LongId(adaptorId);
	}

	@RequestMapping(method = HttpMethod.POST, path = "/protocol-adaptor/remove")
	public void removeProtocolAdaptor(@RequestBody IdParam idParam) {
		protocolAdaptorService.remove(idParam.getId());
	}

}