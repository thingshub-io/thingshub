package io.thingshub.transport.utils;

import lombok.NonNull;

/**
 * <p>
 * String的字节操作工具类
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public abstract class StringOps {

	public static String strFromBytes(@NonNull final byte[] bytes, int start, int len) {
		return new String(bytes, start, len);
	}

	public static byte[] strToBytes(final String str) {
		return str.getBytes();
	}

}