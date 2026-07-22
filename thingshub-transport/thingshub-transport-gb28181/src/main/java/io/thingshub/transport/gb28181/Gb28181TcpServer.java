package io.thingshub.transport.gb28181;

import java.util.Properties;

import javax.sip.ListeningPoint;
import javax.sip.SipFactory;

import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.parser.StringMsgParserFactory;
import io.thingshub.commons.SysException;
import io.thingshub.transport.Server;
import io.thingshub.transport.Transport;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;

/**
 * <p>
 * Gb28181信令服务器
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j(topic = "io.thingshub.transport.server")
public class Gb28181TcpServer implements Server {

	@Inject
	private Gb28181TransportConfig gb28181TransportConfig;

	@Inject
	private Gb28181SipListener gb28181SipListener;

	private DisposableServer disposableServer;

	@Override
	public Transport transport() {
		return Transport.GB28181_TCP;
	}

	@Override
	public String name() {
		return gb28181TransportConfig.getTcpServerName();
	}

	@Override
	public Mono<Server> start() {
		Properties properties = new Properties();
		properties.setProperty("javax.sip.STACK_NAME", "SIP-PROXY");
		properties.setProperty("javax.sip.IP_ADDRESS", gb28181TransportConfig.getHost());
		properties.setProperty("gov.nist.javax.sip.DELIVER_UNSOLICITED_NOTIFY", Boolean.TRUE.toString());
		properties.setProperty("gov.nist.javax.sip.AUTOMATIC_DIALOG_ERROR_HANDLING", Boolean.FALSE.toString());
		properties.setProperty("gov.nist.javax.sip.DELIVER_TERMINATED_EVENT_FOR_NULL_DIALOG", Boolean.TRUE.toString());
		properties.setProperty("gov.nist.javax.sip.COMPUTE_CONTENT_LENGTH_FROM_MESSAGE_BODY", Boolean.TRUE.toString());
		properties.setProperty("gov.nist.javax.sip.RELEASE_REFERENCES_STRATEGY", "Normal");
		properties.setProperty("gov.nist.javax.sip.RELIABLE_CONNECTION_KEEP_ALIVE_TIMEOUT", "15");
		properties.setProperty("gov.nist.javax.sip.REENTRANT_LISTENER", Boolean.TRUE.toString());
		properties.setProperty("gov.nist.javax.sip.THREAD_AUDIT_INTERVAL_IN_MILLISECS", "30000");
		properties.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE", "100");
		properties.setProperty("gov.nist.javax.sip.STACK_LOGGER", "io.github.lunasaw.sip.common.conf.StackLoggerImpl");
		properties.setProperty("gov.nist.javax.sip.SERVER_LOGGER", "io.github.lunasaw.sip.common.conf.ServerLoggerImpl");
		properties.setProperty("gov.nist.javax.sip.LOG_MESSAGE_CONTENT", "true");

		try {
			SipFactory sipFactory = SipFactory.getInstance();
			sipFactory.setPathName("gov.nist");
			SipStackImpl sipStack = (SipStackImpl) sipFactory.createSipStack(properties);
			sipStack.setMessageParserFactory(new StringMsgParserFactory());

			ListeningPoint tcpListeningPoint = sipStack.createListeningPoint(gb28181TransportConfig.getHost(), gb28181TransportConfig.getPort(), "TCP");
			SipProviderImpl tcpSipProvider = (SipProviderImpl) sipStack.createSipProvider(tcpListeningPoint);
			tcpSipProvider.setDialogErrorsAutomaticallyHandled();
			tcpSipProvider.addSipListener(gb28181SipListener);
		} catch (Exception e) {
			throw new SysException(e);
		}

		return bind(gb28181TransportConfig).doOnNext(this::afterBinding).doOnSuccess(this::onSuccess).doOnError(this::onError).thenReturn(this).cast(Server.class);
	}

	private void afterBinding(DisposableServer disposableServer) {
		this.disposableServer = disposableServer;
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (!disposableServer.isDisposed()) {
				disposableServer.dispose();
			}
		}));
	}

	private void onSuccess(DisposableServer disposableServer) {
		log.info("Thingshub GB28181 TCP SIP server has started on port {}", gb28181TransportConfig.getPort());
	}

	private void onError(Throwable e) {
		log.error("Failed to start GB28181 TCP SIP server. Error: ", e);
	}

	@Override
	@PreDestroy
	public void shutdown() {
		if (this.disposableServer != null && !this.disposableServer.isDisposed()) {
			this.disposableServer.dispose();
		}
	}

}
