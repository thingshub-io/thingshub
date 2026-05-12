package io.thingshub.commons.meta.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.thingshub.commons.meta.ValidateResult;
import lombok.Data;
import lombok.Getter;

@Getter
public class BoolType extends DataType {

	public static BoolType fromSpecs(Map<String, Object> specs) {
		BoolType boolType = new BoolType();
		List<BoolOption> options = new ArrayList<>();
		for (Entry<String, Object> entry : specs.entrySet()) {
			BoolOption boolOption = new BoolOption();

			if (entry.getKey().equals(boolType.trueValue) || entry.getKey().equals(boolType.trueValue)) {
				boolOption.setValue(entry.getKey());
				boolOption.setText(entry.getValue().toString());
				options.add(boolOption);
			} else {
				throw new RuntimeException("不支持的布尔值: " + entry.getKey());
			}
		}

		boolType.setBoolOptions(options);

		return boolType;
	}

	private String trueValue = "true";

	private String falseValue = "false";

	private List<BoolOption> boolOptions;

	void setBoolOptions(List<BoolOption> boolOptions) {
		this.boolOptions = boolOptions;
	}

	@Override
	public String getName() {
		return "bool";
	}

	@Override
	public String getTitle() {
		return "布尔值";
	}

	@Override
	public ValidateResult validate(Object value) {
		if (value instanceof Boolean) {
			return ValidateResult.builder().success(true).build();
		}

		String stringVal = String.valueOf(value).trim();
		if (stringVal.equals(trueValue) || stringVal.equals(falseValue)) {
			return ValidateResult.builder().success(true).build();
		}

		return ValidateResult.builder().success(false).error("不支持的布尔值: " + value).build();
	}

	@Data
	public static class BoolOption {

		private String value;

		private String text;

	}

}
