package io.thingshub.transport.throttler;

import static io.thingshub.transport.throttler.ResourceType.TOTAL_INBOUND_BYTES_PER_SECOND;

public class InboundResourceCondition implements Condition {

	private final ResourceThrottler resourceThrottler;

	private final String tenantId;

	public InboundResourceCondition(ResourceThrottler resourceThrottler, String tenantId) {
		this.resourceThrottler = resourceThrottler;
		this.tenantId = tenantId;
	}

	@Override
	public boolean meet() {
		return !resourceThrottler.hasResource(tenantId, TOTAL_INBOUND_BYTES_PER_SECOND);
	}
}
