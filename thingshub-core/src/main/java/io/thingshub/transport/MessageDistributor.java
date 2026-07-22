package io.thingshub.transport;

import org.apache.ignite.IgniteMessaging;

import io.thingshub.ioc.Component;
import jakarta.inject.Inject;

/**
 * <p>
 * Distribute message publication to cluster
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Component
public class MessageDistributor {

	@Inject
	private IgniteMessaging igniteMessaging;

	public void distribute(Publication publication) {
		igniteMessaging.send("publication", publication);
	}
}
