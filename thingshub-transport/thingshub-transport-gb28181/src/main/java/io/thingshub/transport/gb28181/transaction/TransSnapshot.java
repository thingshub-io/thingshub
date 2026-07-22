package io.thingshub.transport.gb28181.transaction;

import javax.sip.RequestEvent;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TransSnapshot {

	private final RequestEvent requestEvent;

	private final String callId;

	private final TransactionType type;

}