package io.thingshub.service;

import java.util.List;

import com.google.common.collect.Lists;

import cn.hutool.db.sql.Condition;
import io.thingshub.commons.ServiceException;
import io.thingshub.entity.MessageAssign;
import io.thingshub.ioc.Service;
import io.thingshub.service.base.BaseService;
import io.thingshub.service.base.IdGenerator;
import jakarta.inject.Inject;

/**
 * <p>
 * Client User's Message Permission Management
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class MessageAssignService extends BaseService<Long, MessageAssign> {

	@Inject
	private IdGenerator idGenerator;

	public List<MessageAssign> getMessageAssigns(String clientUserName, String productCode) {
		return this.query(Lists.newArrayList(new Condition("client_user_name", clientUserName), new Condition("product_code", productCode)));
	}

	public Long assignMessageSpec(String clientUserName, String productCode, Long msgSpecId) throws ServiceException {
		List<Condition> conditions = Lists.newArrayList( //
				new Condition("client_user_name", clientUserName), //
				new Condition("product_code", productCode), //
				new Condition("msg_spec_id", msgSpecId));
		MessageAssign messageAssign = this.getOne(conditions);
		if (messageAssign != null) {
			return messageAssign.getId();
		}

		long id = idGenerator.nextId();
		messageAssign = new MessageAssign();
		messageAssign.setId(id);
		messageAssign.setClientUserName(clientUserName);
		messageAssign.setProductCode(productCode);
		messageAssign.setMsgSpecId(msgSpecId);

		this.save(id, messageAssign);

		return id;
	}

	public void cancelMessageSpec(String clientUserName, String productCode, Long msgSpecId) throws ServiceException {
		List<Condition> conditions = Lists.newArrayList( //
				new Condition("client_user_name", clientUserName), //
				new Condition("product_code", productCode), //
				new Condition("msg_spec_id", msgSpecId));
		this.remove(conditions);
	}

}
