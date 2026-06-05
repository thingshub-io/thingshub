package io.thingshub.dashboard.params;

import io.thingshub.commons.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 消息与收件查询的请求参数
 * </p>
 *
 * @author Albert
 * @since 1.0.0
 */
public class MessageRequestParams {

	@Data
	@EqualsAndHashCode(callSuper = false)
	public static class QueryPublicationParams extends PageParams {

	}

	@Data
	@EqualsAndHashCode(callSuper = false)
	public static class QueryDeliveryParams extends PageParams {

	}

}
