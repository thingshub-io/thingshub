package io.thingshub.commons.meta.types;

import io.thingshub.commons.meta.ValidateResult;
import io.thingshub.commons.model.DataTypeSpecs;

public abstract class DataType {

	/**
	 * Data Type Name
	 */
	public abstract String getName();

	/**
	 * Data Type Title
	 */
	public String getTitle() {
		return null;
	}

	/**
	 * Check if the value is valid
	 * 
	 * @param value
	 * @return
	 */
	public abstract ValidateResult validate(Object value);

	/**
	 * Build data type instance from specifications
	 * 
	 * @param dataTypeSpecs
	 * @return
	 */
	public static DataType from(DataTypeSpecs dataTypeSpecs) {
		return switch (dataTypeSpecs.getType()) {
		case "int" -> IntType.fromSpecs(dataTypeSpecs.getSpecs());
		case "float" -> FloatType.fromSpecs(dataTypeSpecs.getSpecs());
		case "double" -> DoubleType.fromSpecs(dataTypeSpecs.getSpecs());
		case "text" -> TextType.fromSpecs(dataTypeSpecs.getSpecs());
		case "date" -> DateType.fromSpecs(dataTypeSpecs.getSpecs());
		case "enum" -> EnumType.fromSpecs(dataTypeSpecs.getSpecs());
		case "array" -> ArrayType.fromSpecs(dataTypeSpecs.getSpecs());
		case "bool" -> BoolType.fromSpecs(dataTypeSpecs.getSpecs());
		default -> null;
		};
	}

}
