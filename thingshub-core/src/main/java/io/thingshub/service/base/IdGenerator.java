package io.thingshub.service.base;

import cn.hutool.core.lang.Snowflake;

public class IdGenerator extends Snowflake {

	private static final long serialVersionUID = 9054599401471007060L;

	public IdGenerator() {
		super();
	}

	public IdGenerator(long workerId) {
		super(workerId);
	}

	public IdGenerator(long workerId, long dataCenterId) {
		super(workerId, dataCenterId);
	}

}