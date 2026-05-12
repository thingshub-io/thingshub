package io.thingshub.transport.utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import lombok.NonNull;

/**
 * <p>
 * 字节工具类
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
public abstract class ByteOps {

	private static Predicate<byte[]> NOT_EMPTY_BYTE_ARRAY = bytes -> bytes != null && bytes.length > 0;

	public static byte[] concatAll(@NonNull final byte[] first, @NonNull final byte[]... rest) {
		return concatAll(first, NOT_EMPTY_BYTE_ARRAY, rest);
	}

	public static byte[] concatAll(@NonNull final byte[] first, Predicate<byte[]> predicate, @NonNull final byte[]... rest) {
		int totalLength = first.length;
		for (byte[] array : rest) {
			if (predicate.test(array)) {
				totalLength += array.length;
			}
		}
		byte[] result = Arrays.copyOf(first, totalLength);
		int offset = first.length;
		for (byte[] array : rest) {
			if (predicate.test(array)) {
				System.arraycopy(array, 0, result, offset, array.length);
				offset += array.length;
			}
		}
		return result;
	}

	public static byte[] concatAll(@NonNull final List<byte[]> rest) {
		return concatAll(rest, NOT_EMPTY_BYTE_ARRAY);
	}

	public static byte[] concatAll(@NonNull final List<byte[]> rest, Predicate<byte[]> predicate) {
		int totalLength = 0;
		for (byte[] array : rest) {
			if (predicate.test(array)) {
				totalLength += array.length;
			}
		}
		byte[] result = new byte[totalLength];
		int offset = 0;
		for (byte[] array : rest) {
			if (predicate.test(array)) {
				System.arraycopy(array, 0, result, offset, array.length);
				offset += array.length;
			}
		}
		return result;
	}

	/**
	 * 获取字节数组中从指定起始位置开始之后指定数量的字节
	 * 
	 * @param bytes 源字节数组
	 * @param start 起始位置
	 * @param len   字节数量
	 * @return
	 */
	public static byte[] slice(@NonNull final byte[] bytes, final int start, final int len) {
		final byte[] tmp = new byte[len];
		System.arraycopy(bytes, start, tmp, 0, len);
		return tmp;
	}

	/**
	 * 获取字节数组中从指定起始位置开始之后的所有字节
	 * 
	 * @param bytes 源字节数组
	 * @param start 起始位置（包含）
	 * @return
	 */
	public static byte[] slice(@NonNull final byte[] bytes, final int start) {
		int len = bytes.length - start;
		final byte[] tmp = new byte[len];
		System.arraycopy(bytes, start, tmp, 0, len);
		return tmp;
	}
}