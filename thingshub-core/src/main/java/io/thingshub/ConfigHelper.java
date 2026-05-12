package io.thingshub;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.thingshub.ioc.Value;

/**
 * <p>
 * Configs Assembling
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public class ConfigHelper {

	private static final Pattern ANNOTATION_VALUE_PATTERN = Pattern.compile("\\$\\{(.+)\\}");

	public static void injectValue(Map<String, Object> configs, Object bean) throws IllegalAccessException {
		Class<?> clazz = bean.getClass();
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			Value theAnno = field.getAnnotation(Value.class);
			if (theAnno != null) {
				field.setAccessible(true);
				String configName = theAnno.value();
				Matcher matcher = ANNOTATION_VALUE_PATTERN.matcher(configName);
				while (matcher.find()) {
					String[] nameAndDefault = matcher.group(1).split(":");
					String propName = nameAndDefault[0].trim();
					String propDefault = nameAndDefault.length > 1 ? nameAndDefault[1].trim() : null;
					Object propVal = configs.get(propName);
					if (propVal != null) {
						field.set(bean, propVal);
					} else {
						switch (field.getType().getTypeName()) {
						case "java.lang.String" -> {
							field.set(bean, propDefault);
						}
						case "long", "java.lang.Long" -> {
							if (propDefault != null) {
								field.set(bean, Long.valueOf(propDefault));
							}
						}
						case "int", "java.lang.Integer" -> {
							if (propDefault != null) {
								field.set(bean, Integer.valueOf(propDefault));
							}
						}
						case "short", "java.lang.Short" -> {
							if (propDefault != null) {
								field.set(bean, Short.valueOf(propDefault));
							}
						}
						case "byte", "java.lang.Byte" -> {
							if (propDefault != null) {
								field.set(bean, Byte.valueOf(propDefault));
							}
						}
						case "boolean", "java.lang.Boolean" -> {
							if (propDefault != null) {
								field.set(bean, Boolean.valueOf(propDefault));
							}
						}
						case "float", "java.lang.Float" -> {
							if (propDefault != null) {
								field.set(bean, Float.valueOf(propDefault));
							}
						}
						case "double", "java.lang.Double" -> {
							if (propDefault != null) {
								field.set(bean, Double.valueOf(propDefault));
							}
						}
						case "java.math.BigDecimal" -> {
							if (propDefault != null) {
								field.set(bean, new BigDecimal(propDefault));
							}
						}
						case "java.util.Map" -> {
							Map<String, String> entries = new HashMap<>();
							configs.forEach((confKey, confVal) -> {
								if (confKey.startsWith(propName)) {
									entries.put(confKey.substring(propName.concat(".").length()), confVal.toString());
								}
							});
							field.set(bean, entries);
						}
						}
					}
				}
			}
		}
	}
}