package io.thingshub.transport.utils;

import lombok.NonNull;

/**
 * <p>
 * BCD码工具类
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public abstract class BcdOps {

	public static String bcd2Str(@NonNull final byte[] bytes, final int start, final int len) {
		final StringBuilder builder = new StringBuilder(len << 2);
		for (int i = start; i < (start + len); i++) {
			builder.append((bytes[i] & 0xf0) >>> 4);
			builder.append(bytes[i] & 0x0f);
		}

		return builder.toString();
	}

	public static String bcd2Str(final byte[] bytes) {
		return bcd2Str(bytes, 0, bytes.length);
	}

	public static byte[] strToBcd(final String str) {
		// odd -> 0 + str
		final String source = NumberUtils.isOdd(str.length()) ? "0" + str : str;

		final byte[] result = new byte[source.length() >> 1];
		final byte[] sourceBytes = source.getBytes();
		for (int i = 0; i < result.length; i++) {

			byte high = ascii2Bcd(sourceBytes[i << 1]);
			byte low = ascii2Bcd(sourceBytes[(i << 1) + 1]);

			result[i] = (byte) ((high << 4) | low);
		}
		return result;
	}

	private static byte ascii2Bcd(final byte asc) {
		if ((asc >= '0') && (asc <= '9')) {
			return (byte) (asc - '0');
		} else if ((asc >= 'A') && (asc <= 'F')) {
			return (byte) (asc - 'A' + 10);
		} else if ((asc >= 'a') && (asc <= 'f')) {
			return (byte) (asc - 'a' + 10);
		} else {
			return (byte) (asc - 48);
		}
	}
}