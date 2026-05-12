package io.thingshub.transport.utils;

import java.util.Arrays;

import com.google.common.primitives.Bytes;

import lombok.NonNull;

/**
 * <p>
 * 整形数字的字节操作工具类
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public abstract class IntOps {

	private static final int MASK = 0xFF;

	public static byte intToByte(final int i) {
		return (byte) (i & MASK);
	}

	public static byte[] intTo1Bytes(final int i) {
		return new byte[] { intToByte(i) };
	}

	public static int intFrom1Bytes(@NonNull final byte[] bytes, final int startIndex) {
		return bytes[startIndex] & MASK;
	}

	public static int intFromByte(final byte b) {
		return b & MASK;
	}

	public static byte[] intTo2Bytes(final int value) {
		return new byte[] { (byte) ((value >>> 8) & MASK), (byte) (value & MASK) };
	}

	public static int intFrom2Bytes(@NonNull final byte[] bytes, final int startIndex) {
		return ((bytes[startIndex] & MASK) << 8) | (bytes[startIndex + 1] & MASK);
	}

	public static int intFrom2Bytes(@NonNull final byte[] bytes) {
		return intFrom2Bytes(bytes, 0);
	}

	public static byte[] intTo3Bytes(int value) {
		return new byte[] { (byte) ((value >>> 16) & MASK), (byte) ((value >>> 8) & MASK), (byte) (value & MASK) };
	}

	public static int intFrom3Bytes(@NonNull final byte[] bytes, final int startIndex) {
		return ((bytes[startIndex] & MASK) << 16) | ((bytes[startIndex + 1] & MASK) << 8) | ((bytes[startIndex + 2] & MASK));
	}

	public static int intFrom3Bytes(@NonNull final byte[] bytes) {
		return intFrom3Bytes(bytes, 0);
	}

	public static byte[] intTo4Bytes(final int value) {
		return new byte[] { (byte) ((value >>> 24) & MASK), (byte) ((value >>> 16) & MASK), (byte) ((value >>> 8) & MASK), (byte) (value & MASK) };
	}

	public static int intFrom4Bytes(@NonNull final byte[] bytes) {
		return intFrom4Bytes(bytes, 0);
	}

	public static int intFrom4Bytes(@NonNull final byte[] bytes, final int startIndex) {
		return ((bytes[startIndex] & MASK) << 24) | ((bytes[startIndex + 1] & MASK) << 16) | ((bytes[startIndex + 2] & MASK) << 8)
				| ((bytes[startIndex + 3] & MASK));
	}

	public static int intFromBytes(@NonNull final byte[] bytes, int start, int len) {
		return switch (len) {
		case 1 -> intFrom1Bytes(bytes, start);
		case 2 -> intFrom2Bytes(bytes, start);
		case 3 -> intFrom3Bytes(bytes, start);
		case 4 -> intFrom4Bytes(bytes, start);
		default -> throw new IllegalArgumentException("len ∈ [1,4]");
		};
	}

	public static int intFromBytes(@NonNull final byte[] bytes) {
		return intFromBytes(bytes, 0, bytes.length);
	}

	public static void main(String[] args) {
		byte[] bytes = Bytes.concat( //
				intTo1Bytes(1), // 命令码
				intTo2Bytes(2), // 消息序号
				intTo1Bytes(StringOps.strToBytes("D00001").length), // 设备编号长度
				StringOps.strToBytes("D00001"), //
				intTo1Bytes(0), // 加密标识
				intTo2Bytes(61), // 消息体长度

				intTo1Bytes(2), // 桩类型
				intTo4Bytes(10000), // 额定功率
				intTo1Bytes(2), // 充电枪数量
				intTo4Bytes(1001), // 计费规则ID
				intTo4Bytes(2001), // 计费规则版本号
				intTo4Bytes(10201), // 运营商编码
				BcdOps.strToBcd("123abc"), // 密码 3
				StringOps.strToBytes("V2.0.10000000000"), // 设备软件版本号 16
				StringOps.strToBytes("P_DC_20230000000"), // 设备硬件版本号 16
				StringOps.strToBytes("2.000000"), // 通信协议版本号 8

				intTo2Bytes(123) // CRC校验
		);

		System.out.println(bytes);

		System.out.println(Arrays.toString(bytes));
	}

}