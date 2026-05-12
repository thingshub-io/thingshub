package io.thingshub;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Class path资源读取
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

@Slf4j
public abstract class ClassPathResource {

	public static Mono<ByteBuf> readFile(String path) {
		try {
			InputStream inputStream = ClassPathResource.class.getResourceAsStream(path);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] bytes = new byte[1024];
			int n;
			while ((n = bufferedInputStream.read(bytes)) != -1) {
				out.write(bytes, 0, n);
			}

			return Mono.just(PooledByteBufAllocator.DEFAULT.buffer(out.size()).writeBytes(out.toByteArray()));
		} catch (IOException e) {
			log.error("read classpath file error {}", path, e);
		}

		return Mono.empty();
	}

}
