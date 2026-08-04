package io.thingshub.transport.gb28181.processor;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;

import javax.sip.RequestEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipFactory;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.UserAgentHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Response;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.common.collect.Lists;

import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.message.SIPRequest;
import io.thingshub.transport.authenticate.AuthRequest;
import io.thingshub.transport.authenticate.AuthResult;
import io.thingshub.transport.authenticate.AuthResult.ResultCode;
import io.thingshub.transport.authenticate.Authenticator;
import io.thingshub.transport.gb28181.RegisterInfo;
import io.thingshub.transport.gb28181.transaction.Transaction;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * REGISTER请求处理器
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class RegisterRequestProcessor implements SipRequestProcessor {

	@Inject
	private Authenticator authenticator;

	@Override
	public String getMethod() {
		return "REGISTER";
	}

	@Override
	public void process(RequestEvent event) {
		try {
			SIPRequest request = (SIPRequest) event.getRequest();
			int expires = request.getExpires().getExpires();
			boolean isRegister = expires > 0;

			FromHeader fromHeader = (FromHeader) request.getHeader(FromHeader.NAME);
			AddressImpl address = (AddressImpl) fromHeader.getAddress();
			SipUri uri = (SipUri) address.getURI();
			String userId = uri.getUser();

			log.debug("处理{}请求：用户ID = {}, 过期时间 = {}", isRegister ? "注册" : "注销", userId, expires);

			RegisterInfo registerInfo = new RegisterInfo();
			registerInfo.setExpires(expires);
			registerInfo.setRegisterAt(new Date());

			ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
			String transport = reqViaHeader.getTransport();
			registerInfo.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");

			String receiveIp = request.getLocalAddress().getHostAddress();
			String remoteIp = request.getTopmostViaHeader().getReceived();
			int remotePort = request.getTopmostViaHeader().getRPort();
			if (remoteIp == null || remotePort == -1) {
				remoteIp = Optional.ofNullable(request.getPeerPacketSourceAddress()).map(InetAddress::getHostAddress).orElse(request.getViaHost());
				remotePort = OptionalInt.of(request.getPeerPacketSourcePort()).stream().filter(e -> e != 0).findFirst().orElse(request.getViaPort());
			}
			registerInfo.setLocalIp(receiveIp);
			registerInfo.setRemoteIp(remoteIp);
			registerInfo.setRemotePort(remotePort);

			String gbVerHeaderName = "Note";
			String gbVerHeaderVal = getHeaderValueFromRequest(request, gbVerHeaderName);
			registerInfo.setPeerProtocolVersion(gbVerHeaderVal);

			String noteHeaderName = "Note";
			String noteHeaderVal = getHeaderValueFromRequest(request, noteHeaderName);
			registerInfo.setPeerNote(noteHeaderVal);

			String muiHeaderName = "Monitor-User-Identity";
			String muiHeaderVal = getHeaderValueFromRequest(request, muiHeaderName);
			registerInfo.setPeerMonitorUserIdentity(muiHeaderVal);

			Transaction transaction = new Transaction();
			transaction.setCallId(request.getCallIdHeader().getCallId());
			transaction.setFromTag(request.getFromTag());
			transaction.setToTag(request.getToTag());
			transaction.setViaBranch(request.getTopmostViaHeader().getBranch());

			if (!isRegister) {
//				publisher.publishEvent(ServerLifecycleEvent.offline(this, userId, registerInfo, sipTransaction));
			} else {
				AuthorizationHeader authHead = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
				if (authHead == null) {
					Response response = SipFactory.getInstance().createMessageFactory().createResponse(Response.UNAUTHORIZED, request);
					response.setReasonPhrase("Unauthorized");

					long time = Instant.now().toEpochMilli();
					long pad = new Random().nextLong();
					String nonce = DigestUtils.md5Hex(Long.valueOf(time).toString() + Long.valueOf(pad).toString());
					WWWAuthenticateHeader wwwAuthenticateHeader = SipFactory.getInstance().createHeaderFactory().createWWWAuthenticateHeader("Digest");
					wwwAuthenticateHeader.setParameter("realm", "3402000000");
					wwwAuthenticateHeader.setParameter("qop", "auth");
					wwwAuthenticateHeader.setParameter("nonce", nonce);
					wwwAuthenticateHeader.setParameter("algorithm", "MD5");
					response.addHeader(wwwAuthenticateHeader);

					// TODO
					Header xGbVerHeader = SipFactory.getInstance().createHeaderFactory().createHeader("X-GB-Ver", "3.0");
					response.addHeader(xGbVerHeader);

					ServerTransaction serverTransaction = getServerTransaction(event);
					if (serverTransaction != null) {
						log.debug("发送事务响应: transaction={}, response-callId={}, response-cseq={}", transaction, response.getHeader("Call-ID"), response.getHeader("CSeq"));
						serverTransaction.sendResponse(response);
					} else {
						log.warn("无法获取服务器事务，降级到无事务模式发送响应");

						String targetIp;
						if (request.getLocalAddress() != null) {
							targetIp = request.getLocalAddress().getHostAddress();
						} else {
							ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
							if (viaHeader != null) {
								targetIp = viaHeader.getHost();
							} else {
								targetIp = "127.0.0.1";
							}
						}

						if (response.getHeader(UserAgentHeader.NAME) == null) {
							// TODO user agent名称从sip.common.user-agent配置获取

							List<String> agents = Lists.newArrayList("sip-proxy");
							UserAgentHeader userAgentHeader = SipFactory.getInstance().createHeaderFactory().createUserAgentHeader(agents);
							response.addHeader(userAgentHeader);
						}

						ViaHeader viaHeader = (ViaHeader) response.getHeader(ViaHeader.NAME);
						String theTransport = "UDP";
						if (viaHeader != null) {
							theTransport = viaHeader.getTransport();
						}
//						sendWithoutTransaction(response);
					}
//					publisher.publishEvent(ServerLifecycleEvent.challenge(this, userId));
					return;
				}

				AuthResult authResult = authenticator.basicAuth(AuthRequest.builder().clientId(userId).password("").build());
				if (authResult.resultCode() != ResultCode.OK) {
					log.warn("authenticate error. user id: {}", userId);
//					ResponseCmd.sendResponse(Response.FORBIDDEN, "Forbidden", evt);
					return;
				}

//				List<Header> okHeaderList = getRegisterOkHeaderList(request);
//				ResponseCmd.response(Response.OK).phrase("OK").requestEvent(evt).headers(okHeaderList).send();
//				publisher.publishEvent(ServerLifecycleEvent.register(this, userId, registerInfo));
//				publisher.publishEvent(ServerLifecycleEvent.online(this, userId, sipTransaction));
//
//				processRegisterRequest(evt, request, userId, registerInfo, transaction);
			}

		} catch (Exception e) {
//			log.error("处理REGISTER请求异常：evt = {}", evt, e);
		}
	}

	private String getHeaderValueFromRequest(SIPRequest request, String headerName) {
		String val = null;

		Header requestHeader = request.getHeader(headerName);
		if (requestHeader != null) {
			String headerStr = requestHeader.toString();
			int colonIdx = headerStr.indexOf(':');
			if (colonIdx > 0) {
				String headerVal = headerStr.substring(colonIdx + 1).trim();
				if (!headerVal.isEmpty()) {
					val = headerVal;
				}
			}
		}

		return val;
	}

	private ServerTransaction getServerTransaction(RequestEvent event) {
		ServerTransaction tx = event.getServerTransaction();
		if (tx != null) {
			return tx;
		}

		if (event.getRequest() instanceof SIPRequest sipRequest) {
			try {
				Object transaction = sipRequest.getTransaction();
				if (transaction instanceof ServerTransaction serverTransaction) {
					tx = serverTransaction;
				}
			} catch (Exception e) {
				log.debug("从SIPRequest获取事务时发生异常: {}", e.getMessage());
			}
		}

		return tx;
	}

}
