package io.thingshub.service;

import java.util.List;

import com.google.common.collect.Lists;

import cn.hutool.db.sql.Condition;
import io.thingshub.entity.MessageAuthority;
import io.thingshub.ioc.Service;
import io.thingshub.service.base.BaseService;
import io.thingshub.service.base.IdGenerator;
import jakarta.inject.Inject;

/**
 * <p>
 * Message Authority Service
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class MessageAuthorityService extends BaseService<Long, MessageAuthority> {

	@Inject
	private IdGenerator idGenerator;

	public List<MessageAuthority> getMessageAuthorities(String serviceClient) {
		return this.query(Lists.newArrayList(new Condition("service_client", serviceClient)));
	}

	public List<MessageAuthority> getMessageAuthorities(String serviceClient, String productCode) {
		return this.query(Lists.newArrayList(new Condition("service_client", serviceClient), new Condition("product_code", productCode)));
	}

	public void saveMessageAuthority(String serviceClient, String productCode, List<String> messages) {
		messages.forEach(m -> {
			List<Condition> conditions = Lists.newArrayList( //
					new Condition("service_client", serviceClient), //
					new Condition("product_code", productCode), //
					new Condition("message_name", m));
			MessageAuthority messageAssign = this.getOne(conditions);
			if (messageAssign != null) {
				return;
			}

			long id = idGenerator.nextId();
			messageAssign = new MessageAuthority();
			messageAssign.setId(id);
			messageAssign.setServiceClient(serviceClient);
			messageAssign.setProductCode(productCode);
			messageAssign.setMessageName(m);

			this.save(id, messageAssign);
		});
	}

	public void cancelMessageAuthority(String serviceClient, String productCode, String messageName) {
		List<Condition> conditions = Lists.newArrayList( //
				new Condition("service_client", serviceClient), //
				new Condition("product_code", productCode), //
				new Condition("message_name", messageName));
		this.remove(conditions);
	}

}
