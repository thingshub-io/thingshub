package io.thingshub.transport.tcp.processor;

import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_EVENT_POST;
import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_PROPERTY_POST;
import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_SERVICE_FUNCTION_REPLY;
import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_SERVICE_REQUEST_POST;
import static java.util.Optional.ofNullable;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.MDC;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Maps;

import cn.hutool.core.date.DateUtil;
import io.thingshub.commons.MessageType;
import io.thingshub.entity.MessageDefinition;
import io.thingshub.entity.Publication;
import io.thingshub.service.MessageDefinitionService;
import io.thingshub.service.PublicationService;
import io.thingshub.service.base.IdGenerator;
import io.thingshub.transport.ChannelContextWrapper;
import io.thingshub.transport.Processor;
import io.thingshub.transport.PublishWay;
import io.thingshub.transport.tcp.TcpChannelContext;
import io.thingshub.transport.tcp.packet.PublishPacket;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PublishProcessor implements Processor<TcpChannelContext, PublishPacket> {

	@Inject
	protected PublicationService publicationService;

	@Inject
	protected MessageDefinitionService messageDefinitionService;

	@Inject
	private IdGenerator idGenerator;

	@Override
	public void process(@NonNull TcpChannelContext ctx, @NonNull PublishPacket packet) {
		if (log.isDebugEnabled()) {
			log.debug("Client send packet, payload: {}", packet.getPayload());
		}

		final Map<String, String> parentMdc = MDC.getCopyOfContextMap();
		ctx.addFgTask(CompletableFuture.runAsync(() -> distributePublishing(ctx, packet), ctx.executor()).exceptionally(e -> {
			ofNullable(parentMdc).ifPresent(pmdc -> MDC.setContextMap(pmdc));
			log.error("", e);

			return null;
		}));
	}

	private void distributePublishing(TcpChannelContext ctx, PublishPacket msg) {
		String productCode = ctx.channel().attr(ChannelContextWrapper.ATTRIBUTE_PRODUCT_CODE).get();
		String msgName = msg.getPayload().getName();
		MessageDefinition messageDefinition = messageDefinitionService.getMessageDefinition(productCode, msgName);
		if (messageDefinition == null) {
			log.warn("Not supported message name {} and ignore publishing", msg.getPayload().getName());
			return;
		}

		// TODO 校验参数，不合法返回DistributeResult.REJECTED
		String stdTopic = switch (MessageType.of(messageDefinition.getType())) {
		case PROPERTY_POST -> String.format(THING_TOPIC_PROPERTY_POST, productCode, ctx.getClientId(), msgName);
		case SERVICE_FUNCTION_REPLY -> String.format(THING_TOPIC_SERVICE_FUNCTION_REPLY, productCode, ctx.getClientId(), msgName);
		case SERVICE_REQUEST_POST -> String.format(THING_TOPIC_SERVICE_REQUEST_POST, productCode, ctx.getClientId(), msgName);
		case EVENT_POST -> String.format(THING_TOPIC_EVENT_POST, productCode, ctx.getClientId(), msgName);
		default -> "";
		};
		byte[] payloadBytes = (byte[]) TcpChannelContext.pollIncoming(ctx.channel().id().asLongText().concat("_" + msg.getPayload().getId()).concat("_payload"));

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, ctx.getTenantSettings().getMaxMessageExpiryInterval());

		long publicationId = idGenerator.nextId();
		Publication publication = new Publication();
		publication.setId(publicationId);
		publication.setUsername(ctx.getUsername());
		publication.setClientId(ctx.getClientId());
		publication.setPubWay(PublishWay.PUBLISH.value());
		publication.setTopic(stdTopic);
		publication.setStdTopic(stdTopic);
		publication.setProps(Maps.newHashMap());
		publication.setPayload(new String(payloadBytes, StandardCharsets.UTF_8));
		publication.setStdPayload(JSON.toJSONString(msg.getPayload()));
		publication.setExpireTime(calendar.getTime());
		publication.setTimestamp(msg.getPayload().getTime() == null ? System.currentTimeMillis() : DateUtil.parse(msg.getPayload().getTime()).getTime());
		publicationService.save(publicationId, publication, ctx.getTenantSettings().getMaxMessageExpiryInterval(), TimeUnit.SECONDS);
	}

}
