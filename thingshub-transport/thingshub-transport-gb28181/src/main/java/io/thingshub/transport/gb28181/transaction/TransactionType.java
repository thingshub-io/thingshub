package io.thingshub.transport.gb28181.transaction;

public enum TransactionType {
	/**
	 * 显式事务：基于原始RequestEvent
	 */
	EXPLICIT,
	/**
	 * 隐式事务：基于ThreadLocal传递的Call-ID
	 */
	IMPLICIT
}