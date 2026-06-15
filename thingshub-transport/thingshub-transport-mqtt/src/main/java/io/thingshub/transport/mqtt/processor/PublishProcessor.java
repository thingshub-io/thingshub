package io.thingshub.transport.mqtt.processor;

import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.PUBLICATION_EXPIRY_INTERVAL;
import static io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType.REASON_STRING;
import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_EVENT_POST;
import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_PROPERTY_POST;
import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_SERVICE_FUNCTION_REPLY;
import static io.thingshub.commons.ThingshubConstants.THING_TOPIC_SERVICE_REQUEST_POST;
import static io.thingshub.transport.ChannelContextWrapper.ATTRIBUTE_CLIENT_TYPE;
import static io.thingshub.transport.ChannelContextWrapper.ATTRIBUTE_PRODUCT_CODE;
import static io.thingshub.transport.mqtt.handler.v5.MQTT5DisconnectReasonCode.QuotaExceeded;
import static io.thingshub.transport.mqtt.handler.v5.MQTT5DisconnectReasonCode.ServerBusy;
import static io.thingshub.transport.mqtt.handler.v5.MQTT5PubAckReasonCode.NoMatchingSubscribers;
import static io.thingshub.transport.mqtt.handler.v5.MQTT5PubAckReasonCode.Success;
import static io.thingshub.transport.mqtt.handler.v5.MQTT5PubAckReasonCode.UnspecifiedError;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.ignite.IgniteMessaging;

import com.alibaba.fastjson2.JSON;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttProperties.StringProperty;
import io.netty.handler.codec.mqtt.MqttPubReplyMessageVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.thingshub.acl.AclAction;
import io.thingshub.acl.AclManager;
import io.thingshub.commons.MessageType;
import io.thingshub.commons.ThingshubMessage;
import io.thingshub.entity.MessageDefinition;
import io.thingshub.service.MessageDefinitionService;
import io.thingshub.service.base.IdGenerator;
import io.thingshub.service.model.ClientType;
import io.thingshub.subscribe.SubscriptionManager;
import io.thingshub.transport.DistributeResult;
import io.thingshub.transport.Processor;
import io.thingshub.transport.Publication;
import io.thingshub.transport.PublishWay;
import io.thingshub.transport.mqtt.MqttChannelContext;
import io.thingshub.transport.mqtt.handler.v5.MQTT5PubRecReasonCode;
import io.thingshub.transport.mqtt.packet.PublishPacket;
import io.thingshub.transport.mqtt.service.Retain;
import io.thingshub.transport.mqtt.service.RetainService;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * MQTT PUBLISH processor
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public class PublishProcessor implements Processor<MqttChannelContext, PublishPacket> {

	@Inject
	private AclManager aclManager;

	@Inject
	private RetainService retainManager;

	@Inject
	private SubscriptionManager subscriptionManager;

	@Inject
	private MessageDefinitionService messageDefinitionService;

	@Inject
	private IdGenerator idGenerator;

	@Inject
	private IgniteMessaging igniteMessaging;

	public static enum RetainResult {
		RETAINED, CLEARED, EXCEED_LIMIT, REJECTED, ERROR
	};

	public record PubResult(DistributeResult distributeResult, RetainResult retainResult) {
	};

	@Override
	public void process(MqttChannelContext ctx, PublishPacket packet) {
		String theTopic = packet.getTopic();
		Integer theQos = (Integer) packet.getProperties().get("qos");
		if (log.isDebugEnabled()) {
			log.debug("Client send PUBLISH packet, topic: {}, qos:{}", theTopic, theQos);
		}

		try {
			if (!aclManager.check(ctx.getClientId(), theTopic, AclAction.PUBLISH)) {
				log.warn("No publish permission is granted to topic: {}", theTopic);

				if (Integer.parseInt(ctx.getProtocolVersion()) < 5) {
					ctx.goAway(false);
				} else {
					// TODO farewell
					// MQTT5DisconnectReasonCode.NotAuthorized
					// MQTT5DisconnectReasonCode.UnspecifiedError
				}

				return;
			}

			MqttQoS qos = MqttQoS.valueOf(theQos);
			switch (qos) {
			case AT_MOST_ONCE -> {
				try {
					PubResult pubResult = doPub(ctx, packet);
					if (pubResult.distributeResult() == DistributeResult.REJECTED || pubResult.retainResult() == RetainResult.REJECTED) {
						log.warn("Server busy: too many QoS0 publish");

						if (Integer.parseInt(ctx.getProtocolVersion()) < 5) {
							ctx.goAway(false);
						} else {
							MqttProperties props = new MqttProperties();
							props.add(new StringProperty(REASON_STRING.value(), "Too many QoS0 publish"));
							MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(ServerBusy.value()).properties(props).build();

							ctx.goAwayWithFarewell(farewell, false);
						}
					}
				} catch (Exception e) {
					log.error("", e);
				}
			}
			case AT_LEAST_ONCE -> {
				if (!ctx.isPacketIdUsing(packet.getPacketId())) {
					ctx.incReceivingCount();
					ctx.addUsingPacketId(packet.getPacketId());

					try {
						PubResult pubResult = doPub(ctx, packet);
						if (ctx.channel().isActive()) {
							if (ctx.channel().isWritable()) {
								MqttMessage pubAckMsg = null;
								if (Integer.parseInt(ctx.getProtocolVersion()) < 5) {
									if (pubResult.distributeResult() == DistributeResult.REJECTED || pubResult.retainResult() == RetainResult.REJECTED) {
										log.warn("Server busy: too many QoS1 publishing");
										ctx.goAway(false);

										return;
									} else {
										pubAckMsg = MqttMessageBuilders.pubAck().packetId(packet.getPacketId()).build();
									}
								} else {
									if (pubResult.distributeResult() == DistributeResult.REJECTED || pubResult.retainResult() == RetainResult.REJECTED) {
										log.warn("Server busy: too many QoS1 publishing");

										MqttProperties props = new MqttProperties();
										props.add(new StringProperty(REASON_STRING.value(), "Too many QoS1 publishing"));
										MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(ServerBusy.value()).properties(props).build();

										ctx.goAwayWithFarewell(farewell, false);

										return;
									} else if (pubResult.retainResult() == RetainResult.EXCEED_LIMIT) {
										MqttProperties props = new MqttProperties();
										props.add(new StringProperty(REASON_STRING.value(), "Retain resource throttled"));
										pubAckMsg = MqttMessageBuilders.pubAck().packetId(packet.getPacketId()).reasonCode(QuotaExceeded.value()).properties(props).build();
									} else if (pubResult.distributeResult() == DistributeResult.OK) {
										pubAckMsg = MqttMessageBuilders.pubAck().packetId(packet.getPacketId()).reasonCode(Success.value()).build();
									} else if (pubResult.distributeResult() == DistributeResult.NO_MATCH) {
										pubAckMsg = MqttMessageBuilders.pubAck().packetId(packet.getPacketId()).reasonCode(NoMatchingSubscribers.value()).build();
									} else {
										pubAckMsg = MqttMessageBuilders.pubAck().packetId(packet.getPacketId()).reasonCode(UnspecifiedError.value()).build();
									}
								}

								if (pubAckMsg != null) {
									ctx.writeAndFlush(pubAckMsg).addListener(new ChannelFutureListener() {

										@Override
										public void operationComplete(ChannelFuture future) throws Exception {
											if (!future.isSuccess()) {
												log.error("Sending PUBACK packet failed");
												if (future.cause() != null) {
													log.error("", future.cause());
												}
											}
										}

									});
								} else {
									log.warn("No PUBACK packet build");
								}
							} else {
								log.error("Channel is not writable and sending PUBREC packet for publishing is dropped");
							}
						} else {
							log.error("Channel is inactive and sending PUBACK packet for publishing is dropped");
						}
					} catch (Exception e) {
						log.error("", e);
					} finally {
						ctx.decReceivingCount();
						ctx.removeUsingPacketId(packet.getPacketId());
					}
				} else {
					log.info("Packet id {} is in using, ignored", packet.getPacketId());
				}
			}
			case EXACTLY_ONCE -> {
				assert ctx.executor().inEventLoop();

				if (ctx.isPacketIdUsing(packet.getPacketId())) {
					log.error("Protocol violation: packet id is in using [MQTT3-2.3.1-4]");

					if (Integer.parseInt(ctx.getProtocolVersion()) < 5) {
						ctx.goAway(false);
					} else {
						MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 2);
						MqttMessageIdVariableHeader varHeader = new MqttPubReplyMessageVariableHeader(packet.getPacketId(), MQTT5PubRecReasonCode.PacketIdentifierInUse.value(),
								new MqttProperties());
						ctx.writeAndFlush(new MqttMessage(fixedHeader, varHeader));
					}
				} else {
					ctx.incReceivingCount();
					ctx.addUsingPacketId(packet.getPacketId());
					MqttChannelContext.bufferIncoming(ctx.getChannelId().concat("_" + packet.getPacketId()).concat("_pub"), packet.getPacketId());

					boolean abortFlag = false;

					try {
						PubResult pubResult = doPub(ctx, packet);

						if (ctx.channel().isActive()) {
							if (ctx.channel().isWritable()) {
								if (pubResult.distributeResult() == DistributeResult.REJECTED || pubResult.distributeResult() == DistributeResult.ERROR) {
									abortFlag = true;
								}

								MqttMessage pubRecMsg = null;
								if (Integer.parseInt(ctx.getProtocolVersion()) < 5) {
									if (pubResult.distributeResult() == DistributeResult.REJECTED || pubResult.retainResult() == RetainResult.REJECTED) {
										log.warn("Server busy: too many QoS2 publishing");

										abortFlag = true;
										ctx.goAway(false);
									} else {
										MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 2);
										MqttMessageIdVariableHeader varHeader = MqttMessageIdVariableHeader.from(packet.getPacketId());
										pubRecMsg = new MqttMessage(fixedHeader, varHeader);
									}
								} else {
									if (pubResult.distributeResult() == DistributeResult.REJECTED || pubResult.retainResult() == RetainResult.REJECTED) {
										log.warn("Server busy: too many QoS2 publishing");

										MqttProperties props = new MqttProperties();
										props.add(new StringProperty(REASON_STRING.value(), "Too many QoS2 publishing"));
										MqttMessage farewell = MqttMessageBuilders.disconnect().reasonCode(ServerBusy.value()).properties(props).build();

										abortFlag = true;
										ctx.goAwayWithFarewell(farewell, false);
									} else if (pubResult.retainResult() == RetainResult.EXCEED_LIMIT) {
										MqttProperties props = new MqttProperties();
										props.add(new StringProperty(REASON_STRING.value(), "Retain resource throttled"));

										MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 2);
										MqttMessageIdVariableHeader varHeader = new MqttPubReplyMessageVariableHeader(packet.getPacketId(),
												MQTT5PubRecReasonCode.QuotaExceeded.value(), props);
										pubRecMsg = new MqttMessage(fixedHeader, varHeader);
									} else if (pubResult.distributeResult() == DistributeResult.OK) {
										MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 2);
										MqttMessageIdVariableHeader varHeader = new MqttPubReplyMessageVariableHeader(packet.getPacketId(), MQTT5PubRecReasonCode.Success.value(),
												new MqttProperties());
										pubRecMsg = new MqttMessage(fixedHeader, varHeader);
									} else if (pubResult.distributeResult() == DistributeResult.NO_MATCH) {
										MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 2);
										MqttMessageIdVariableHeader varHeader = new MqttPubReplyMessageVariableHeader(packet.getPacketId(),
												MQTT5PubRecReasonCode.NoMatchingSubscribers.value(), new MqttProperties());
										pubRecMsg = new MqttMessage(fixedHeader, varHeader);
									} else {
										MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 2);
										MqttMessageIdVariableHeader varHeader = new MqttPubReplyMessageVariableHeader(packet.getPacketId(),
												MQTT5PubRecReasonCode.UnspecifiedError.value(), new MqttProperties());
										pubRecMsg = new MqttMessage(fixedHeader, varHeader);
									}
								}

								if (pubRecMsg != null) {
									ctx.writeAndFlush(pubRecMsg).addListener(new ChannelFutureListener() {

										@Override
										public void operationComplete(ChannelFuture future) throws Exception {
											if (!future.isSuccess()) {
												ctx.decReceivingCount();
												ctx.removeUsingPacketId(packet.getPacketId());

												log.error("Sending PUBREC packet[id:{}] failed", packet.getPacketId());
												if (future.cause() != null) {
													log.error("", future.cause());
												}
											}
										}

									});
								}
							} else {
								abortFlag = true;

								log.error("Sending PUBREC packet for publishing is dropped because buffer is overflow");
							}
						} else {
							abortFlag = true;

							log.error("Sending PUBREC packet for publishing is dropped");
						}
					} catch (

					Exception e) {
						abortFlag = true;

						log.error("", e);
					} finally {
						if (abortFlag) {
							ctx.decReceivingCount();
							ctx.removeUsingPacketId(packet.getPacketId());

							MqttChannelContext.pollIncoming(ctx.getChannelId().concat("_" + packet.getPacketId()).concat("_pub"));
						}
					}
				}
			}
			default -> {
				log.warn("Invalid MQTT QoS: {}", qos.value());
				ctx.goAway(false);
			}
			}
		} finally {
			MqttChannelContext.pollIncoming(ctx.channel().id().asLongText().concat("_" + packet.getPacketId()).concat("_payload"));
		}
	}

	private PubResult doPub(MqttChannelContext ctx, PublishPacket packet) {
		DistributeResult distResult = distributePublication(ctx, packet);
		if (!(Boolean) packet.getProperties().get("isRetain")) {
			return new PubResult(distResult, RetainResult.RETAINED);
		} else {
			RetainResult retainResult = retainPublication(ctx, packet);
			return new PubResult(distResult, retainResult);
		}
	}

	private DistributeResult distributePublication(MqttChannelContext ctx, PublishPacket packet) {
		ThingshubMessage stdMessage = packet.getPayload();
		if (stdMessage == null) {
			return DistributeResult.OK;
		}

		String msgName = stdMessage.getName();
		ClientType clientType = ctx.channel().attr(ATTRIBUTE_CLIENT_TYPE).get();
		String rawTopic = packet.getTopic();
		String stdTopic = null;
		if (clientType == ClientType.DEVICE_CLIENT) {
			String productCode = ctx.channel().attr(ATTRIBUTE_PRODUCT_CODE).get();

			MessageDefinition messageDefinition = messageDefinitionService.getMessageDefinition(productCode, msgName);
			if (messageDefinition == null) {
				log.warn("Not supported message name {} and ignore publishing", msgName);
				return DistributeResult.ERROR;
			} else {
				// TODO 校验参数，不合法返回DistributeResult.REJECTED
				stdTopic = switch (MessageType.of(messageDefinition.getType())) {
				case PROPERTY_POST -> String.format(THING_TOPIC_PROPERTY_POST, productCode, ctx.getClientId(), msgName);
				case SERVICE_FUNCTION_REPLY -> String.format(THING_TOPIC_SERVICE_FUNCTION_REPLY, productCode, ctx.getClientId(), msgName);
				case SERVICE_REQUEST_POST -> String.format(THING_TOPIC_SERVICE_REQUEST_POST, productCode, ctx.getClientId(), msgName);
				case EVENT_POST -> String.format(THING_TOPIC_EVENT_POST, productCode, ctx.getClientId(), msgName);
				default -> "";
				};
			}
		} else if (clientType == ClientType.SERVICE_CLIENT) {
			stdTopic = rawTopic;
		}

		byte[] rawPayloadBytes = (byte[]) MqttChannelContext.pollIncoming(ctx.channel().id().asLongText().concat("_" + packet.getPacketId()).concat("_payload"));

		long publicationId = idGenerator.nextId();
		Publication publication = new Publication();
		publication.setId(publicationId);
		publication.setUsername(ctx.getUsername());
		publication.setClientId(ctx.getClientId());
		publication.setPubWay(PublishWay.PUBLISH.value());
		publication.setTopic(rawTopic);
		publication.setStdTopic(stdTopic);
		publication.setProps(packet.getProperties());
		publication.setPayload(new String(rawPayloadBytes, StandardCharsets.UTF_8));
		publication.setStdPayload(JSON.toJSONString(packet.getPayload()));
		igniteMessaging.send("publication", publication);

		if ((Integer) packet.getProperties().get("qos") > 0) {
			if (!subscriptionManager.hasMatch(stdTopic)) {
				return DistributeResult.NO_MATCH;
			}
		}

		return DistributeResult.OK;
	}

	private RetainResult retainPublication(MqttChannelContext ctx, PublishPacket packet) {
		String rawTopic = packet.getTopic();
		if (packet.getPayload() == null) {
			retainManager.cleanRetain(rawTopic);

			return RetainResult.CLEARED;
		} else {
//				 if (!resourceThrottler.hasResource(clientInfo.getTenantId(), TotalRetainMessageSpaceBytes)) {
//					 RetainResult.EXCEED_LIMIT
//				 }
//				 if (!resourceThrottler.hasResource(clientInfo.getTenantId(), TotalRetainTopics)) {
//					 RetainResult.EXCEED_LIMIT
//				 }
//				if (!resourceThrottler.hasResource(clientInfo.getTenantId(), TotalRetainedMessagesPerSeconds)) {
//					RetainResult.EXCEED_LIMIT
//				}
//				if (!resourceThrottler.hasResource(clientInfo.getTenantId(), TotalRetainedBytesPerSecond)) {
//					RetainResult.EXCEED_LIMIT
//				}

			ThingshubMessage stdMessage = packet.getPayload();

			ClientType clientType = ctx.channel().attr(ATTRIBUTE_CLIENT_TYPE).get();
			String stdTopic = null;
			if (clientType == ClientType.DEVICE_CLIENT) {
				String productCode = ctx.channel().attr(ATTRIBUTE_PRODUCT_CODE).get();
				String msgName = stdMessage.getName();

				MessageDefinition messageDefinition = messageDefinitionService.getMessageDefinition(productCode, msgName);
				if (messageDefinition == null) {
					log.warn("Not supported message name {} and ignore retaining", msgName);
					return RetainResult.ERROR;
				} else {
					stdTopic = switch (MessageType.of(messageDefinition.getType())) {
					case PROPERTY_POST -> String.format(THING_TOPIC_PROPERTY_POST, productCode, ctx.getClientId(), msgName);
					case SERVICE_FUNCTION_REPLY -> String.format(THING_TOPIC_SERVICE_FUNCTION_REPLY, productCode, ctx.getClientId(), msgName);
					case SERVICE_REQUEST_POST -> String.format(THING_TOPIC_SERVICE_REQUEST_POST, productCode, ctx.getClientId(), msgName);
					case EVENT_POST -> String.format(THING_TOPIC_EVENT_POST, productCode, ctx.getClientId(), msgName);
					default -> null;
					};
				}
			} else if (clientType == ClientType.SERVICE_CLIENT) {
				stdTopic = rawTopic;
			}

			Object expiryInterval = packet.getProperties().get(String.valueOf(PUBLICATION_EXPIRY_INTERVAL.value()));
			int messageExpiryInterval = Math.min(expiryInterval == null ? Integer.MAX_VALUE : Integer.valueOf(expiryInterval.toString()),
					ctx.getTenantSettings().getMaxMessageExpiryInterval());
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.SECOND, messageExpiryInterval);

			long retainId = idGenerator.nextId();

			Retain retain = new Retain();
			retain.setId(retainId);
			retain.setTopic(rawTopic);
			retain.setStdTopic(stdTopic);
			retain.setPayload(JSON.toJSONString(packet.getPayload()));
			retain.setProps(packet.getProperties());
			retain.setPublisherId(ctx.getClientId());
			retain.setExpireTime(calendar.getTime());
			retain.setCreateTime(new Date());

			retainManager.saveRetain(retain, messageExpiryInterval, TimeUnit.SECONDS);
		}

		return RetainResult.RETAINED;
	}

}
