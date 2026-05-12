package io.thingshub.commons.meta.types;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import cn.hutool.core.date.DateUtil;
import io.thingshub.commons.meta.ValidateResult;

public class DateType extends DataType {

	private ZoneId zoneId = ZoneId.systemDefault();

	public static DateType fromSpecs(Map<String, Object> specs) {
		return new DateType();
	}

	@Override
	public String getName() {
		return "date";
	}

	@Override
	public String getTitle() {
		return "日期时间";
	}

	@Override
	public ValidateResult validate(Object value) {
		Date date = null;
		if (value instanceof Instant) {
			date = Date.from(((Instant) value));
		} else if (value instanceof LocalDateTime) {
			date = Date.from(((LocalDateTime) value).atZone(zoneId).toInstant());
		} else if (value instanceof Date) {
			date = ((Date) value);
		} else if (value instanceof Number) {
			date = new Date(((Number) value).longValue());
		}
		if (value instanceof String) {
			date = DateUtil.parse((String) value);
		}

		if (date == null) {
			return ValidateResult.builder().success(false).error("非法的日期时间值[" + value + "]").build();
		}

		return ValidateResult.builder().success(true).build();
	}

}
