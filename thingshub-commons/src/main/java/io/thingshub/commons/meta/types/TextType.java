package io.thingshub.commons.meta.types;

import java.util.Map;

import io.thingshub.commons.meta.ValidateResult;
import lombok.Getter;

@Getter
public class TextType extends DataType {

	public static TextType fromSpecs(Map<String, Object> specs) {
		TextType txtType = new TextType();
		if (specs != null) {
			txtType.setLength((Integer) specs.get("length"));
		}

		return txtType;
	}

	private Integer length;

	void setLength(Integer length) {
		this.length = length;
	}

	@Override
	public String getName() {
		return "text";
	}

	@Override
	public String getTitle() {
		return "文本";
	}

	@Override
	public ValidateResult validate(Object value) {
		if (length != null && value != null) {
			if (value.toString().getBytes().length > length) {
				return ValidateResult.builder().success(false).error("超过最大长度:" + length).build();
			}
		}

		return ValidateResult.builder().success(true).build();
	}

}
