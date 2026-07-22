package io.thingshub.transport.gb28181;

import java.util.Iterator;
import java.util.Map;

import javax.sip.Dialog;

import com.google.common.collect.Maps;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DialogRegistry {

	public static final String KIND_INVITE = "INVITE";

	public static final String KIND_SUBSCRIBE = "SUBSCRIBE";

	public static final long NO_EXPIRY = Long.MAX_VALUE;

	private static final Map<String, Entry> BY_CALL_ID = Maps.newConcurrentMap();

	private DialogRegistry() {
	}

	/**
	 * Dialog 注册项。INVITE 与 SUBSCRIBE 共用同一注册表，但通过 kind 区分清理策略。
	 */
	@Getter
	@AllArgsConstructor
	public static final class Entry {

		private final Dialog dialog;

		/**
		 * 业务侧期望的过期时刻（ms）。INVITE = NO_EXPIRY；SUBSCRIBE = now + (expires + grace)*1000
		 */
		private final long expiresAtMs;

		/** "INVITE" or "SUBSCRIBE" */
		private final String kind;

		private final long createTimeMs;
	}

	public static void register(String callId, Dialog dialog) {
		register(callId, dialog, NO_EXPIRY, KIND_INVITE);
	}

	/**
	 * 注册 dialog。同 callId 重复注册会覆盖，记 warn 日志便于排查重复 INVITE / SUBSCRIBE 场景。
	 *
	 * @param callId    Call-ID
	 * @param dialog    JAIN-SIP Dialog
	 * @param expiresAt 业务侧期望过期时刻（ms）；INVITE 用 {@link #NO_EXPIRY}
	 * @param kind      {@link #KIND_INVITE} 或 {@link #KIND_SUBSCRIBE}
	 */
	public static void register(String callId, Dialog dialog, long expiresAt, String kind) {
		if (callId == null || dialog == null) {
			return;
		}
		Entry entry = new Entry(dialog, expiresAt, kind, System.currentTimeMillis());
		Entry prev = BY_CALL_ID.put(callId, entry);
		if (prev != null && prev.dialog != dialog) {
			log.warn("Dialog 覆盖注册: callId={}, prevState={}, prevKind={}, newKind={}", callId, prev.dialog.getState(), prev.kind, kind);
		}
	}

	/**
	 * 获取 dialog，可能为 null（callId 未建过 dialog 或已 terminate）。
	 */
	public static Dialog get(String callId) {
		if (callId == null) {
			return null;
		}
		Entry entry = BY_CALL_ID.get(callId);
		return entry == null ? null : entry.dialog;
	}

	/**
	 * 获取注册项（含 expires / kind 元数据），可能为 null。
	 */
	public static Entry getEntry(String callId) {
		return callId == null ? null : BY_CALL_ID.get(callId);
	}

	/**
	 * 移除 dialog（一般由 processDialogTerminated 钩子或 cleanupExpired 调用）。
	 */
	public static Dialog remove(String callId) {
		if (callId == null) {
			return null;
		}
		Entry removed = BY_CALL_ID.remove(callId);
		return removed == null ? null : removed.dialog;
	}

	/**
	 * 按 callId + dialog 身份移除 —— 仅当注册项持有的 dialog 与传入 dialog 为<b>同一对象</b>时才移除。
	 *
	 * <p>
	 * 自环场景（同一 JVM、同一 {@code SipStack} 既发 INVITE 又收 INVITE）下，出站 UAC dialog 与 入站 UAS
	 * dialog <b>共享同一 callId</b>，但只有 UAC dialog 注册在本表。若按 callId 盲删
	 * （{@link #remove(String)}），UAS 腿先终结时会触发 {@code DialogTerminatedEvent} 误删仍存活、
	 * 等待发 BYE 的 UAC 项，导致后续 BYE 抛 {@link DialogNotFoundException}。本方法比对 dialog 对象
	 * 身份，仅在终结的正是注册项本身时移除，避免交叉误删。
	 *
	 * @param callId Call-ID
	 * @param dialog 真正终结的 JAIN-SIP Dialog
	 * @return 实际被移除的 dialog；未命中（callId 无项 / dialog 不匹配）返回 null
	 */
	public static Dialog remove(String callId, Dialog dialog) {
		if (callId == null || dialog == null) {
			return null;
		}
		Dialog[] holder = new Dialog[1];
		BY_CALL_ID.computeIfPresent(callId, (k, entry) -> {
			if (entry.dialog == dialog) {
				holder[0] = entry.dialog;
				return null; // 身份匹配 → 移除
			}
			return entry; // 非注册项（如自环 UAS 腿）→ 保留
		});
		return holder[0];
	}

	public static int size() {
		return BY_CALL_ID.size();
	}

	/**
	 * 按 kind 统计，便于监控（如 SUBSCRIBE 占比异常告警）。
	 */
	public static int sizeByKind(String kind) {
		if (kind == null) {
			return 0;
		}
		int count = 0;
		for (Entry e : BY_CALL_ID.values()) {
			if (kind.equals(e.kind)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 清理已过期 entry —— SUBSCRIBE 主清理路径。
	 *
	 * <p>
	 * JAIN-SIP 在 SUBSCRIBE 订阅 expires 自然超时（无续订）时不会触发 DialogTerminatedEvent，
	 * 必须由本方法定时清理，否则 DialogRegistry 会持续增长。INVITE 类型用 NO_EXPIRY 标记， 本方法不清理。
	 *
	 * @return 清理掉的 entry 数
	 */
	public static int cleanupExpired() {
		long now = System.currentTimeMillis();
		int cleaned = 0;
		Iterator<Map.Entry<String, Entry>> it = BY_CALL_ID.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Entry> e = it.next();
			Entry entry = e.getValue();
			if (entry.expiresAtMs != NO_EXPIRY && now > entry.expiresAtMs) {
				it.remove();
				cleaned++;
				log.debug("DialogRegistry.cleanupExpired: callId={}, kind={}, expiredAt={}", e.getKey(), entry.kind, entry.expiresAtMs);
			}
		}
		return cleaned;
	}

	/**
	 * 仅供测试调用 —— 清空整个注册表。
	 */
	public static void clearForTest() {
		BY_CALL_ID.clear();
	}
}