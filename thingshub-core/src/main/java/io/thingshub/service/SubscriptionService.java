package io.thingshub.service;

import org.apache.ignite.cache.affinity.AffinityKeyMapped;

import io.thingshub.entity.Subscription;
import io.thingshub.ioc.Service;
import io.thingshub.service.SubscriptionService.SubKey;
import io.thingshub.service.base.BaseService;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * Subscription Service
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Service
public class SubscriptionService extends BaseService<SubKey, Subscription> {

	@AllArgsConstructor
	@Getter
	public static class SubKey {

		private Long id;

		@AffinityKeyMapped
		private String clientId;

	}

}
