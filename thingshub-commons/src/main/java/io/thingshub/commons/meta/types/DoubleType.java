package io.thingshub.commons.meta.types;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Map.Entry;

import io.thingshub.commons.meta.ValueUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DoubleType extends NumberType<Float> {

	public static DoubleType fromSpecs(Map<String, Object> specs) {
		DoubleType doubleType = new DoubleType();
		for (Entry<String, Object> entry : specs.entrySet()) {
			switch (entry.getKey()) {
			case "max":
				try {
					BigDecimal decimal = new BigDecimal(((String) entry.getValue()));
					doubleType.setMax(decimal.setScale(2, RoundingMode.HALF_UP).doubleValue());
				} catch (NumberFormatException e) {
					log.error("", e);
				}
				break;
			case "min":
				try {
					BigDecimal decimal = new BigDecimal(((String) entry.getValue()));
					doubleType.setMin(decimal.setScale(2, RoundingMode.HALF_UP).doubleValue());
				} catch (NumberFormatException e) {
					log.error("", e);
				}
				break;
			case "unitSymbol":
			case "unitTitle":
				ValueUnit valueUnit = ValueUnit.builder().symbol((String) specs.get("unitSymbol")).title((String) specs.get("unitTitle")).build();
				doubleType.setValueUnit(valueUnit);
				break;
			}
		}

		return doubleType;
	}

	@Override
	public String getName() {
		return "double";
	}

	@Override
	public String getTitle() {
		return "双精度浮点数";
	}

	@Override
	public Integer getScale() {
		return 2;
	}

	@Override
	protected Float castNumber(Number number) {
		return number.floatValue();
	}

}
