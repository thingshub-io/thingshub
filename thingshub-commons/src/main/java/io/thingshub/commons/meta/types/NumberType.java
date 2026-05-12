package io.thingshub.commons.meta.types;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.function.Function;

import io.thingshub.commons.meta.ValidateResult;
import io.thingshub.commons.meta.ValueUnit;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public abstract class NumberType<N extends Number> extends DataType {

	private Number max;

	private Number min;

	private ValueUnit valueUnit;

	private Integer scale;

	private RoundingMode roundingMode = RoundingMode.HALF_UP;

	public NumberType<N> max(Number max) {
		this.max = max;
		return this;
	}

	public NumberType<N> min(Number min) {
		this.min = min;
		return this;
	}

	public NumberType<N> unit(ValueUnit valueUnit) {
		this.valueUnit = valueUnit;
		return this;
	}

	public NumberType<N> scale(Integer scale) {
		this.scale = scale;
		return this;
	}

	public NumberType<N> round(RoundingMode roundingMode) {
		this.roundingMode = roundingMode;
		return this;
	}

	@Override
	public ValidateResult validate(Object value) {
		try {
			Number numberValue = convertNumber(value, getScale(), getRoundingMode(), this::castNumber);
			if (numberValue == null) {
				return ValidateResult.builder().success(false).error("数字格式错误:" + value).build();
			}
			if (getMax() != null && numberValue.doubleValue() > getMax().doubleValue()) {
				return ValidateResult.builder().success(false).error("超过最大值:" + getMax()).build();
			}
			if (getMin() != null && numberValue.doubleValue() < getMin().doubleValue()) {
				return ValidateResult.builder().success(false).error("小于最小值:" + getMin()).build();
			}

			return ValidateResult.builder().success(true).build();
		} catch (NumberFormatException e) {
			return ValidateResult.builder().success(false).error(e.getMessage()).build();
		}
	}

	public N convertNumber(Object value, Integer scale, RoundingMode mode, Function<Number, N> mapper) {
		BigDecimal decimal;
		if (value instanceof Number && scale == null) {
			return mapper.apply(((Number) value));
		}
		if (value instanceof String) {
			try {
				value = new BigDecimal(((String) value));
			} catch (NumberFormatException e) {
				return null;
			}
		}

		if (value instanceof Date) {
			value = new BigDecimal(((Date) value).getTime());
		}

		if (!(value instanceof BigDecimal)) {
			try {
				decimal = new BigDecimal(String.valueOf(value));
			} catch (Throwable err) {
				return null;
			}
		} else {
			decimal = ((BigDecimal) value);
		}

		if (scale == null) {
			return mapper.apply(decimal);
		}

		return mapper.apply(decimal.setScale(scale, mode));
	}

	protected abstract N castNumber(Number number);

}
