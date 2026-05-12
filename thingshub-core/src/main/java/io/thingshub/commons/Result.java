package io.thingshub.commons;

import java.io.Serializable;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Result<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 返回信息码，默认0 0：成功，其它:错误码
	 */
	@Builder.Default
	private Integer code = 0;

	/**
	 * 返回信息，默认success
	 */
	@Builder.Default
	private String msg = "success";

	/**
	 * 服务器时间戳
	 */
	private long timestamp;

	/**
	 * 数据对象
	 */
	private T data;

	public static Result<Void> success() {
		return Result.<Void>builder().timestamp(System.currentTimeMillis()).build();
	}

	public static <D> Result<D> success(D data) {
		return Result.<D>builder().data(data).timestamp(System.currentTimeMillis()).build();
	}

	public static Result<Void> error(Integer code, String msg) {
		return Result.<Void>builder().code(code).msg(msg).timestamp(System.currentTimeMillis()).build();
	}

}
