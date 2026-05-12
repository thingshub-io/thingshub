package io.thingshub.commons.meta.types;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSON;

import io.thingshub.commons.meta.ValidateResult;

public class ArrayType extends DataType {

	public static ArrayType fromSpecs(Map<String, Object> specs) {
		DataType elementType = switch ((String) specs.get("elementType")) {
		case "int" -> new IntType();
		case "float" -> new FloatType();
		case "double" -> new DoubleType();
		case "text" -> new TextType();
		default -> null;
		};

		ArrayType arrayType = new ArrayType();
		arrayType.setElementType(elementType);

		return arrayType;
	}

	private DataType elementType;

	private int size;

	void setElementType(DataType elementType) {
		this.elementType = elementType;
	}

	void setSize(int size) {
		this.size = size;
	}

	@Override
	public String getName() {
		return "array";
	}

	@Override
	public String getTitle() {
		return "数组";
	}

	@SuppressWarnings("unchecked")
	@Override
	public ValidateResult validate(Object value) {
		List<Object> elements = null;
		if (value instanceof List) {
			elements = (List<Object>) value;
		} else if (value instanceof String) {
			elements = JSON.parseArray(String.valueOf(value));
		}

		for (Object data : elements) {
			ValidateResult result = elementType.validate(data);
			if (!result.isSuccess()) {
				return result;
			}
		}

		return ValidateResult.builder().success(true).build();
	}

}
