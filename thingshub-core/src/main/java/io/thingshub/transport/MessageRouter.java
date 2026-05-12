package io.thingshub.transport;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.netty.channel.ChannelHandlerContext;
import io.thingshub.ioc.Component;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Transport Message Router
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
@Component
public class MessageRouter {

	private final Map<Class<TransportPacket>, Processor<ChannelHandlerContext, TransportPacket>> messageProcessors = new HashMap<>();

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Inject
	public MessageRouter(Set<Processor> processors) {
		processors.forEach(processor -> {
			Type[] types = processor.getClass().getGenericInterfaces();
			Type[] typeArgs = ((ParameterizedType) types[0]).getActualTypeArguments();
			messageProcessors.put((Class<TransportPacket>) typeArgs[1], processor);
		});
	}

	public void accept(@NonNull ChannelHandlerContext ctx, @NonNull TransportPacket packet) {
		try {
			Optional.ofNullable(messageProcessors.get(packet.getClass())).ifPresent(processor -> processor.process(ctx, packet));
		} catch (Exception e) {
			log.error("Routing packet failed. Error: ", e);
		}
	}

}
