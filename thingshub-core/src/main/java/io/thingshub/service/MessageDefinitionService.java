package io.thingshub.service;

import static io.thingshub.service.base.BaseService.DeletedStatus.DELETED;
import static io.thingshub.service.base.BaseService.DeletedStatus.NOT_DELETED;

import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import cn.hutool.db.sql.Condition;
import io.thingshub.commons.ServiceException;
import io.thingshub.commons.model.MessageType;
import io.thingshub.entity.MessageDefinition;
import io.thingshub.ioc.Service;
import io.thingshub.service.base.BaseService;
import io.thingshub.service.base.IdGenerator;
import jakarta.inject.Inject;

/**
 * <p>
 * Product Message Definition Service
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class MessageDefinitionService extends BaseService<Long, MessageDefinition> {

	@Inject
	private MessageAuthorityService messageAuthorityService;

	@Inject
	private IdGenerator idGenerator;

	public List<MessageDefinition> getDownwardMessages(String productCode) {
		List<MessageDefinition> functionCallMessages = this.query(Lists.newArrayList( //
				new Condition("product_code", productCode), //
				new Condition("type", MessageType.SERVICE_FUNCTION_CALL.name()), //
				new Condition("deleted_status", DeletedStatus.NOT_DELETED.value())));

		List<MessageDefinition> requestReplyMessages = this.query(Lists.newArrayList( //
				new Condition("product_code", productCode), //
				new Condition("type", MessageType.SERVICE_REQUEST_REPLY.name()), //
				new Condition("deleted_status", DeletedStatus.NOT_DELETED.value())));

		return Lists.newArrayList(Iterables.concat(functionCallMessages, requestReplyMessages));
	}

	public List<MessageDefinition> getMessageDefinitions(String productCode) {
		return this.query(Lists.newArrayList(new Condition("product_code", productCode), new Condition("deleted_status", NOT_DELETED.value())));
	}

	public List<MessageDefinition> getAuthorizedMessages(String username, String productCode) {
		List<String> messageNames = messageAuthorityService.getMessageAuthorities(username, productCode).stream().map(a -> a.getMessageName()).toList();

		return this.query(Lists.newArrayList(new Condition("product_code", productCode), new Condition("deleted_status", NOT_DELETED.value()))).stream()
				.filter(s -> messageNames.contains(s.getName())).toList();
	}

	public MessageDefinition getMessageDefinition(String productCode, String name) {
		List<Condition> conditions = Lists.newArrayList(new Condition("product_code", productCode), new Condition("name", name),
				new Condition("deleted_status", NOT_DELETED.value()));
		return this.getOne(conditions);
	}

	public Long createMessageDefinition(MessageDefinition messageDefinition) throws ServiceException {
		if (messageDefinition.getType().equals(MessageType.PROPERTY_POST.name())) {
			List<Condition> checkConditions = Lists.newArrayList( //
					new Condition("product_code", messageDefinition.getProductCode()), //
					new Condition("name", messageDefinition.getName()), //
					new Condition("deleted_status", NOT_DELETED.value()));
			if (this.getOne(checkConditions) != null) {
				throw new ServiceException("消息名称已存在");
			}
		} else {
			List<Condition> checkConditions = Lists.newArrayList( //
					new Condition("product_code", messageDefinition.getProductCode()), //
					new Condition("name", messageDefinition.getName()), //
					new Condition("model_identifier", messageDefinition.getModelIdentifier()), //
					new Condition("deleted_status", NOT_DELETED.value()));
			if (this.getOne(checkConditions) != null) {
				throw new ServiceException("消息名称已存在");
			}
		}

		long id = idGenerator.nextId();
		messageDefinition.setId(id);
		this.save(id, messageDefinition);

		return id;
	}

	public void updateMessageDefinition(MessageDefinition messageDefinition) throws ServiceException {
		if (messageDefinition.getType().equals(MessageType.PROPERTY_POST.name())) {
			List<Condition> checkConditions = Lists.newArrayList( //
					new Condition("name", messageDefinition.getName()), //
					new Condition("deleted_status", NOT_DELETED.value()));
			MessageDefinition theMessageDefinition = this.getOne(checkConditions);
			if (theMessageDefinition != null && !theMessageDefinition.getId().equals(messageDefinition.getId())) {
				throw new ServiceException("消息名称已存在");
			}
		} else {
			List<Condition> checkConditions = Lists.newArrayList( //
					new Condition("name", messageDefinition.getName()), //
					new Condition("model_identifier", messageDefinition.getModelIdentifier()), //
					new Condition("deleted_status", NOT_DELETED.value()));
			MessageDefinition theMessageDefinition = this.getOne(checkConditions);
			if (theMessageDefinition != null && !theMessageDefinition.getId().equals(messageDefinition.getId())) {
				throw new ServiceException("消息名称已存在");
			}
		}

		this.save(messageDefinition.getId(), messageDefinition);
	}

	public void remove(Long id) {
		MessageDefinition messageDefinition = this.getByKey(id);
		if (messageDefinition != null) {
			messageDefinition.setDeletedStatus(DELETED.value());

			this.save(id, messageDefinition);
		}
	}

}
