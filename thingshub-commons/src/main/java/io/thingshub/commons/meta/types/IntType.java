package io.thingshub.commons.meta.types;

import java.util.Map;
import java.util.Map.Entry;

import io.thingshub.commons.meta.ValueUnit;

public class IntType extends NumberType<Integer> {

	public static IntType fromSpecs(Map<String, Object> specs) {
		IntType intType = new IntType();
		for (Entry<String, Object> entry : specs.entrySet()) {
			switch (entry.getKey()) {
			case "max":
				intType.setMax(null);
				break;
			case "min":
				intType.setMin(null);
				break;
			case "unitSymbol":
			case "unitTitle":
				ValueUnit valueUnit = ValueUnit.builder().symbol((String) specs.get("unitSymbol")).title((String) specs.get("unitTitle")).build();
				intType.setValueUnit(valueUnit);
				break;
			}
		}

		return intType;
	}

	@Override
	public String getName() {
		return "int";
	}

	@Override
	public String getTitle() {
		return "整型";
	}

	@Override
	public Integer getScale() {
		return 0;
	}

	@Override
	protected Integer castNumber(Number number) {
		return number.intValue();
	}

}
