package io.thingshub.commons.meta.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.thingshub.commons.meta.ValidateResult;
import lombok.Data;

public class EnumType extends DataType {

	public static EnumType fromSpecs(Map<String, Object> specs) {
		List<Element> elements = new ArrayList<>();
		for (Entry<String, Object> entry : specs.entrySet()) {
			Element element = new Element();
			element.setValue(entry.getKey());
			element.setText(entry.getValue().toString());
			elements.add(element);
		}

		EnumType enumType = new EnumType();
		enumType.setElements(elements);

		return enumType;
	}

	private List<Element> elements;

	void setElements(List<Element> elements) {
		this.elements = elements;
	}

	@Override
	public String getName() {
		return "enum";
	}

	@Override
	public String getTitle() {
		return "枚举";
	}

	@Override
	public ValidateResult validate(Object value) {
		if (elements == null) {
			return ValidateResult.builder().success(false).error("非法枚举值[" + value + "]").build();
		}

		return elements.stream() //
				.filter(ele -> ele.value.equals(String.valueOf(value))) //
				.findFirst().map(e -> ValidateResult.builder().success(true).build()) //
				.orElseGet(() -> ValidateResult.builder().success(false).error("非法枚举[" + value + "]").build());
	}

	@Data
	public static class Element {

		private String value;

		private String text;

		private String description;

		public Map<String, Object> toMap() {
			Map<String, Object> map = new HashMap<>();
			map.put("value", value);
			map.put("text", text);
			map.put("description", description);

			return map;
		}
	}

}
