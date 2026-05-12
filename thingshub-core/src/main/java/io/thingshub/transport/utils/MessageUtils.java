package io.thingshub.transport.utils;

import io.netty.buffer.ByteBuf;

/**
 * <p>
 * 消息的原始字节操作
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public abstract class MessageUtils {

	/**
	 * 获取释放消息字节数组
	 *
	 * @param byteBuf 消息ByteBuf
	 * @return 字节数组
	 */
	public static byte[] copyReleaseByteBuf(ByteBuf byteBuf) {
		byte[] bytes = new byte[byteBuf.readableBytes()];
		byteBuf.readBytes(bytes);
		byteBuf.resetReaderIndex();
		return bytes;
	}

	/**
	 * 获取释放消息字节数组
	 *
	 * @param byteBuf 消息ByteBuf
	 * @return 字节数组
	 */
	public static byte[] copyByteBuf(ByteBuf byteBuf) {
		byte[] bytes = new byte[byteBuf.readableBytes()];
		byteBuf.resetReaderIndex();
		byteBuf.readBytes(bytes);
		byteBuf.resetReaderIndex();
		return bytes;
	}

	/**
	 * 获取释放消息字节数组
	 *
	 * @param byteBuf 消息ByteBuf
	 * @return 字节数组
	 */
	public static byte[] readByteBuf(ByteBuf byteBuf) {
		byte[] bytes = new byte[byteBuf.readableBytes()];
		byteBuf.resetReaderIndex();
		byteBuf.readBytes(bytes);
		byteBuf.resetReaderIndex();
		return bytes;
	}

}