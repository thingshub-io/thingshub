package io.thingshub.transport.utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

public abstract class UTF8Utils {

	private static final int MAX_STRING_LENGTH = 65535;

	public static boolean isWellFormed(String str, boolean sanityCheck) {
		int len = str.length();
		if (len == 0) {
			return true;
		}

		if (len > MAX_STRING_LENGTH) {
			return false;
		}

		char cl = str.charAt(0);
		if (cl == '\u0000') {
			return false;
		}

		if (sanityCheck && isUnacceptableChar(cl)) {
			return false;
		}

		for (int i = 1; i < len; i++) {
			final char cr = str.charAt(i);
			if (cr == '\u0000') {
				return false;
			}
			if (Character.isSurrogatePair(cl, cr)) {
				return false;
			}
			if (sanityCheck && isUnacceptableChar(cr)) {
				return false;
			}
			cl = cr;
		}

		return true;
	}

	public static boolean isValidUTF8Payload(ByteBuffer payload) {
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

		try {
			CharBuffer charBuffer = decoder.decode(payload);
			for (int i = 0; i < charBuffer.length(); i++) {
				char ch = charBuffer.get(i);
				if (isUnacceptableChar(ch)) {
					return false;
				}
			}
		} catch (Throwable e) {
			return false;
		}

		return true;
	}

	public static boolean isValidUTF8Payload(byte[] payload) {
		return isValidUTF8Payload(ByteBuffer.wrap(payload));
	}

	private static boolean isUnacceptableChar(char ch) {
		return Character.isISOControl(ch) || !Character.isDefined(ch);
	}
}