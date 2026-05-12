package io.thingshub.commons.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderBy implements Serializable {

	private static final long serialVersionUID = 6995770798457631515L;

	/**
	 * 需要进行排序的字段名称
	 */
	private String col;

	/**
	 * 是否正序排列
	 */
	private boolean asc = true;

	public static OrderBy asc(String col) {
		return build(col, true);
	}

	public static OrderBy desc(String col) {
		return build(col, false);
	}

	public static List<OrderBy> ascOrders(String... cols) {
		return Arrays.stream(cols).map(OrderBy::asc).toList();
	}

	public static List<OrderBy> descOrders(String... cols) {
		return Arrays.stream(cols).map(OrderBy::desc).toList();
	}

	private static OrderBy build(String col, boolean asc) {
		return new OrderBy(col, asc);
	}
}