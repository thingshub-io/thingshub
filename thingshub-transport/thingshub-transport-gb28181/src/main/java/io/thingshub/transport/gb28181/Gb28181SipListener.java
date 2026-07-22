package io.thingshub.transport.gb28181;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.thingshub.ioc.Component;
import io.thingshub.transport.gb28181.processor.SipRequestProcessor;
import io.thingshub.transport.gb28181.processor.SipResponseProcessor;
import io.thingshub.transport.gb28181.processor.SipTimeoutProcessor;
import io.thingshub.transport.gb28181.subscribe.Event;
import io.thingshub.transport.gb28181.subscribe.EventResult;
import io.thingshub.transport.gb28181.subscribe.SubscribeManager;
import io.thingshub.transport.gb28181.transaction.Transaction;
import io.thingshub.transport.gb28181.transaction.TransactionContext;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Gb28181SipListener implements SipListener {

	private final Map<String, List<SipRequestProcessor>> request_processors = Maps.newHashMap();

	private final Map<String, List<SipResponseProcessor>> response_processors = Maps.newHashMap();

	private final Map<String, List<SipTimeoutProcessor>> timeout_processors = Maps.newHashMap();

	@Inject
	public Gb28181SipListener(Set<SipRequestProcessor> requestProcessors, Set<SipResponseProcessor> responseProcessors, Set<SipTimeoutProcessor> timeoutProcessors) {
		requestProcessors.forEach(processor -> {
			request_processors.computeIfAbsent(processor.getMethod(), m -> Lists.newArrayList()).add(processor);
		});

		responseProcessors.forEach(processor -> {
			response_processors.computeIfAbsent(processor.getMethod(), m -> Lists.newArrayList()).add(processor);
		});

		timeoutProcessors.forEach(processor -> {
			timeout_processors.computeIfAbsent("timeout", m -> Lists.newArrayList()).add(processor);
		});
	}

	@Override
	public void processRequest(RequestEvent requestEvent) {
		String method = requestEvent.getRequest().getMethod();

		Transaction theTransaction = Transaction.from(requestEvent);
		if (theTransaction != null) {
			TransactionContext.setTransaction(theTransaction);
		}

		List<SipRequestProcessor> requestProcessors = request_processors.get(method);
		if (requestProcessors == null) {
			log.warn("不支持的method：{}", method);
			return;
		}

		try {
			ServerTransaction serverTransaction = null;
			if (!"ACK".equals(method)) {
				try {
					serverTransaction = requestEvent.getServerTransaction();
					if (serverTransaction == null) {
						SipProvider sipProvider = (SipProvider) requestEvent.getSource();
						serverTransaction = sipProvider.getNewServerTransaction(requestEvent.getRequest());
					}
				} catch (TransactionAlreadyExistsException e) {
					try {
						serverTransaction = requestEvent.getServerTransaction();
					} catch (Exception ex) {
						log.warn("重新获取现有事务失败: ", ex);
					}
				} catch (TransactionUnavailableException e) {
					log.warn("事务不可用，method：{}", method, e);
				} catch (Exception e) {
					log.warn("创建服务器事务时发生未知错误，method：{}", method, e);

					try {
						serverTransaction = requestEvent.getServerTransaction();
					} catch (Exception ex) {
						log.warn("创建服务器事务时发生未知错误，尝试获取现有事务时失败，method：{}", method, e);
					}
				}
			}

			for (SipRequestProcessor requestProcessor : requestProcessors) {
				requestProcessor.process(requestEvent, serverTransaction);
			}
		} catch (Exception e) {
			log.error("process sip request message error, request event: {}", requestEvent, e);
		} finally {
			TransactionContext.clear();
		}
	}

	@Override
	public void processResponse(ResponseEvent responseEvent) {
		Response response = responseEvent.getResponse();
		int status = response.getStatusCode();

		if ((status >= Response.OK && status < Response.MULTIPLE_CHOICES) || status == Response.UNAUTHORIZED) {
			CSeqHeader cseqHeader = (CSeqHeader) responseEvent.getResponse().getHeader(CSeqHeader.NAME);
			String method = cseqHeader.getMethod();

			List<SipResponseProcessor> responseProcessors = response_processors.get(method);
			if (responseProcessors != null) {
				for (SipResponseProcessor sipResponseProcessor : responseProcessors) {
					if (sipResponseProcessor.isNeedProcess(responseEvent)) {
						sipResponseProcessor.process(responseEvent);
					}
				}
			}

			if (status != Response.UNAUTHORIZED && responseEvent.getResponse() != null && SubscribeManager.getOkSubscribesSize() > 0) {
				SubscribeManager.publishOkEvent(responseEvent);
			}
		} else if ((status >= Response.TRYING) && (status < Response.OK)) {
			// TODO
		} else {
			log.warn("接收到失败的response响应！status：" + status + ",message:" + response.getReasonPhrase() + " response = {}", responseEvent.getResponse());

			if (responseEvent.getResponse() != null && SubscribeManager.getErrorSubscribesSize() > 0) {
				CallIdHeader callIdHeader = (CallIdHeader) responseEvent.getResponse().getHeader(CallIdHeader.NAME);
				if (callIdHeader != null) {
					Event subscribe = SubscribeManager.getErrorSubscribe(callIdHeader.getCallId());
					if (subscribe != null) {
						EventResult eventResult = new EventResult(responseEvent);
						subscribe.response(eventResult);
						SubscribeManager.removeErrorSubscribe(callIdHeader.getCallId());
					}
				}
			}
			if (responseEvent.getDialog() != null) {
				responseEvent.getDialog().delete();
			}
		}
	}

	@Override
	public void processTimeout(TimeoutEvent timeoutEvent) {
		ClientTransaction clientTransaction = timeoutEvent.getClientTransaction();
		if (clientTransaction == null) {
			return;
		}

		Request request = clientTransaction.getRequest();
		CSeqHeader cseqHeader = (CSeqHeader) request.getHeader(CSeqHeader.NAME);
		String method = cseqHeader.getMethod();

		List<SipTimeoutProcessor> timeoutProcessors = timeout_processors.get(method);
		if (timeoutProcessors != null) {
			for (SipTimeoutProcessor timeoutProcessor : timeoutProcessors) {
				timeoutProcessor.process(timeoutEvent);
			}
		}

		CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
		if (callIdHeader != null) {
			Event subscribe = SubscribeManager.getErrorSubscribe(callIdHeader.getCallId());
			EventResult eventResult = new EventResult(timeoutEvent);
			if (subscribe != null) {
				subscribe.response(eventResult);
			}
			SubscribeManager.removeOkSubscribe(callIdHeader.getCallId());
			SubscribeManager.removeErrorSubscribe(callIdHeader.getCallId());
		}
	}

	@Override
	public void processIOException(IOExceptionEvent exceptionEvent) {
		log.error("IO exception event: {} ", JSON.toJSONString(exceptionEvent));
	}

	@Override
	public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
		EventResult eventResult = new EventResult(transactionTerminatedEvent);

		Event timeOutSubscribe = SubscribeManager.getErrorSubscribe(eventResult.getCallId());
		if (timeOutSubscribe != null) {
			timeOutSubscribe.response(eventResult);
		}
	}

	@Override
	public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
		Dialog dialog = dialogTerminatedEvent.getDialog();
		if (dialog != null) {
			CallIdHeader callIdHeader = dialog.getCallId();
			String callId = callIdHeader != null ? callIdHeader.getCallId() : null;
			if (callId != null) {
				// 身份校验移除：自环（同栈既发又收 INVITE）下 UAC/UAS 共享 callId，
				// 仅当终结的正是注册项本身才移除，避免 UAS 腿终结误删等待 BYE 的 UAC 项
				Dialog removed = DialogRegistry.remove(callId, dialog);
				if (removed != null) {
					log.debug("DialogTerminatedEvent 清理 DialogRegistry: callId={}", callId);
				} else {
					log.debug("DialogTerminatedEvent 未匹配注册项（自环对端腿或已清理），保留 registry: callId={}", callId);
				}
			}
		}

		EventResult eventResult = new EventResult(dialogTerminatedEvent);

		Event timeOutSubscribe = SubscribeManager.getErrorSubscribe(eventResult.getCallId());
		if (timeOutSubscribe != null) {
			timeOutSubscribe.response(eventResult);
		}
	}

}
