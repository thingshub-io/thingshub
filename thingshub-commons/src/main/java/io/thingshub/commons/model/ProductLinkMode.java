package io.thingshub.commons.model;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 产品联网模式
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public enum ProductLinkMode {

	ETHERNET(1, "以太网连接"), CELL(2, "蜂窝数据网连接"), WIFI(3, "WIFI连接"), OTHER(9, "其它");

	@Accessors(fluent = true)
	@Getter
	private final int type;

	@Accessors(fluent = true)
	@Getter
	private final String title;

	ProductLinkMode(int type, String title) {
		this.type = type;
		this.title = title;
	}

	public static ProductLinkMode of(int type) {
		return switch (type) {
		case 1 -> ETHERNET;
		case 2 -> CELL;
		case 3 -> WIFI;
		case 9 -> OTHER;
		default -> null;
		};
	}

}
