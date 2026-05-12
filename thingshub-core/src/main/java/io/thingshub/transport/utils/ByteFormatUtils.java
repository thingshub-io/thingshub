package io.thingshub.transport.utils;

import java.text.DecimalFormat;

/**
 * <p>
 * 字节格式化工具类
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */
public abstract class ByteFormatUtils {

	public static String formatByte(long byteNumber) {
		double format = 1024.0;
		double kbNumber = byteNumber / format;
		if (kbNumber < format) {
			return new DecimalFormat("#.##KB").format(kbNumber);
		}
		double mbNumber = kbNumber / format;
		if (mbNumber < format) {
			return new DecimalFormat("#.##MB").format(mbNumber);
		}
		double gbNumber = mbNumber / format;
		if (gbNumber < format) {
			return new DecimalFormat("#.##GB").format(gbNumber);
		}
		double tbNumber = gbNumber / format;
		return new DecimalFormat("#.##TB").format(tbNumber);
	}

	public static String formatByte(double byteNumber) {
		double format = 1024.0;
		double kbNumber = byteNumber / format;
		if (kbNumber < format) {
			return new DecimalFormat("#.##KB").format(kbNumber);
		}
		double mbNumber = kbNumber / format;
		if (mbNumber < format) {
			return new DecimalFormat("#.##MB").format(mbNumber);
		}
		double gbNumber = mbNumber / format;
		if (gbNumber < format) {
			return new DecimalFormat("#.##GB").format(gbNumber);
		}
		double tbNumber = gbNumber / format;
		return new DecimalFormat("#.##TB").format(tbNumber);
	}
}
