package io.thingshub.commons.model;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 产品类型
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public enum ProductType {

	DIRECT(1, "直连设备"), GATEWAY(2, "网关设备"), GATEWAY_SUB(3, "网关子设备"), MONITOR(4, "监控设备"), OTHER(9, "其它");

	@Accessors(fluent = true)
	@Getter
	private final int type;

	@Accessors(fluent = true)
	@Getter
	private final String title;

	ProductType(int type, String title) {
		this.type = type;
		this.title = title;
	}

	public static ProductType of(int type) {
		return switch (type) {
		case 1 -> DIRECT;
		case 2 -> GATEWAY;
		case 3 -> GATEWAY_SUB;
		case 4 -> MONITOR;
		default -> null;
		};
	}

}
