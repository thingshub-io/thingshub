package io.thingshub.transport.tcp.processor;

import static io.thingshub.commons.model.MessageType.REPLY;
import static io.thingshub.commons.model.MessageType.REQUEST;
import static io.thingshub.commons.model.ThingModelType.EVENT;
import static io.thingshub.commons.model.ThingModelType.PROPERTY;
import static io.thingshub.commons.model.ThingModelType.SERVICE;
import static io.thingshub.subscribe.TopicUtils.THING_TOPIC_PREFIX;
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
import io.thingshub.entity.MessageSpec;
import io.thingshub.entity.Publication;
import io.thingshub.service.MessageSpecService;
import io.thingshub.service.PublicationService;
import io.thingshub.service.base.IdGenerator;
import io.thingshub.transport.ChannelContextWrapper;
import io.thingshub.transport.Processor;
import io.thingshub.transport.PubMethod;
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
	protected MessageSpecService messageSpecService;

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
		String subTopic = "";

		MessageSpec messageSpec = messageSpecService.getMessageSpec(msg.getPayload().getName());
		if (messageSpec == null) {
			log.warn("Not supported message name {} and will be ignored", msg.getPayload().getName());
			return;
		} else if (messageSpec.getCat().equals(PROPERTY.name())) {
			if (messageSpec.getType().equals(REPLY.name())) {
				log.warn("Wrong message name {} and will be ignored", msg.getPayload().getName());
				return;
			} else {
				subTopic = subTopic.concat(PROPERTY.name().toLowerCase()).concat("/post/").concat(msg.getPayload().getName());
			}
		} else if (messageSpec.getCat().equals(SERVICE.name())) {
			if (messageSpec.getType().equals(REQUEST.name())) {
				log.warn("Wrong message name {} and will be ignored", msg.getPayload().getName());
				return;
			} else {
				subTopic = subTopic.concat(SERVICE.name().toLowerCase()).concat("/call/reply/").concat(msg.getPayload().getName());
			}
		} else if (messageSpec.getCat().equals(EVENT.name())) {
			if (messageSpec.getType().equals(REPLY.name())) {
				log.warn("Wrong message name {} and will be ignored", msg.getPayload().getName());
				return;
			} else {
				subTopic = subTopic.concat(EVENT.name().toLowerCase()).concat("/post/").concat(msg.getPayload().getName());
			}
		}

		String stdTopic = THING_TOPIC_PREFIX.concat("/thing/").concat(productCode + "/" + ctx.getClientId() + "/").concat(subTopic);
		byte[] payloadBytes = (byte[]) TcpChannelContext.pollIncoming(ctx.channel().id().asLongText().concat("_" + msg.getPayload().getId()).concat("_payload"));

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, ctx.getTenantSettings().getMaxMessageExpiryInterval());

		long publicationId = idGenerator.nextId();
		Publication publication = new Publication();
		publication.setId(publicationId);
		publication.setPublisherId(ctx.getClientId());
		publication.setPubMethod(PubMethod.PUBLISH.value());
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
