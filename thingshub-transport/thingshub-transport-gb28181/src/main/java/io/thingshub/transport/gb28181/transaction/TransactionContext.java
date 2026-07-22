package io.thingshub.transport.gb28181.transaction;

import javax.sip.RequestEvent;

public class TransactionContext {

	/**
	 * 线程本地存储原始请求事件（显式模式）
	 */
	private static final ThreadLocal<RequestEvent> REQUEST_EVENT_HOLDER = new ThreadLocal<>();

	/**
	 * 线程本地存储完整事务信息（隐式模式）
	 */
	private static final ThreadLocal<Transaction> TRANSACTION_HOLDER = new ThreadLocal<>();

	/**
	 * 线程本地存储Call-ID（向后兼容）
	 */
	private static final ThreadLocal<String> CALL_ID_HOLDER = new ThreadLocal<>();

	/**
	 * 线程本地存储事务类型标识
	 */
	private static final ThreadLocal<TransactionType> TRANSACTION_TYPE_HOLDER = new ThreadLocal<>();

	public static void setRequestEvent(RequestEvent requestEvent) {
		if (requestEvent != null) {
			REQUEST_EVENT_HOLDER.set(requestEvent);
			TRANSACTION_TYPE_HOLDER.set(TransactionType.EXPLICIT);

			Transaction transaction = Transaction.from(requestEvent);
			TRANSACTION_HOLDER.set(transaction);
			CALL_ID_HOLDER.set(transaction.getCallId());
		}
	}

	public static RequestEvent getRequestEvent() {
		return REQUEST_EVENT_HOLDER.get();
	}

	public static void setTransaction(Transaction transaction) {
		if (transaction != null) {
			TRANSACTION_HOLDER.set(transaction);
			if (REQUEST_EVENT_HOLDER.get() == null) {
				TRANSACTION_TYPE_HOLDER.set(TransactionType.IMPLICIT);
			}
			if (transaction.getCallId() != null) {
				CALL_ID_HOLDER.set(transaction.getCallId());
			}
		}
	}

	public static void setCallId(String callId) {
		if (callId != null && !callId.trim().isEmpty()) {
			CALL_ID_HOLDER.set(callId.trim());
			if (REQUEST_EVENT_HOLDER.get() == null) {
				TRANSACTION_TYPE_HOLDER.set(TransactionType.IMPLICIT);
			}
		}
	}

	public static Transaction getCurrentTransaction() {
		Transaction transaction = null;
		RequestEvent requestEvent = REQUEST_EVENT_HOLDER.get();
		if (requestEvent != null) {
			transaction = Transaction.from(requestEvent);
		}

		if (transaction == null) {
			transaction = TRANSACTION_HOLDER.get();
		}

		return transaction;
	}

	public static String getCurrentCallId() {
		String callId = null;
		RequestEvent requestEvent = REQUEST_EVENT_HOLDER.get();
		if (requestEvent != null) {
			callId = requestEvent.getRequest().getHeader("Call-ID").toString();
		}

		if (callId == null) {
			callId = CALL_ID_HOLDER.get();
		}

		return callId;
	}

	public static boolean hasActiveTransaction() {
		return REQUEST_EVENT_HOLDER.get() != null || CALL_ID_HOLDER.get() != null;
	}

	public static TransactionType getCurrentTransactionType() {
		return TRANSACTION_TYPE_HOLDER.get();
	}

	public static boolean isExplicitTransaction() {
		return TransactionType.EXPLICIT.equals(TRANSACTION_TYPE_HOLDER.get());
	}

	public static boolean isImplicitTransaction() {
		return TransactionType.IMPLICIT.equals(TRANSACTION_TYPE_HOLDER.get());
	}

	public static void clear() {
		REQUEST_EVENT_HOLDER.remove();
		TRANSACTION_HOLDER.remove();
		CALL_ID_HOLDER.remove();
		TRANSACTION_TYPE_HOLDER.remove();
	}

	public static TransSnapshot snapshot() {
		RequestEvent requestEvent = REQUEST_EVENT_HOLDER.get();
		String callId = CALL_ID_HOLDER.get();
		TransactionType type = TRANSACTION_TYPE_HOLDER.get();

		return new TransSnapshot(requestEvent, callId, type);
	}

	public static void restore(TransSnapshot transSnapshot) {
		if (transSnapshot.getRequestEvent() != null) {
			REQUEST_EVENT_HOLDER.set(transSnapshot.getRequestEvent());
		}
		if (transSnapshot.getCallId() != null) {
			CALL_ID_HOLDER.set(transSnapshot.getCallId());
		}
		if (transSnapshot.getType() != null) {
			TRANSACTION_TYPE_HOLDER.set(transSnapshot.getType());
		}
	}

	public static String getDebugInfo() {
		RequestEvent requestEvent = REQUEST_EVENT_HOLDER.get();
		String callId = CALL_ID_HOLDER.get();
		TransactionType type = TRANSACTION_TYPE_HOLDER.get();

		StringBuilder sb = new StringBuilder();
		sb.append("SipTransactionContext{");
		sb.append("thread=").append(Thread.currentThread().getName());
		sb.append(", type=").append(type);
		sb.append(", hasRequestEvent=").append(requestEvent != null);
		sb.append(", callId='").append(callId != null ? callId : getCurrentCallId()).append("'");
		sb.append("}");

		return sb.toString();
	}

}