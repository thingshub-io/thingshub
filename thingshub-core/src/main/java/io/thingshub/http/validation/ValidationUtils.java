package io.thingshub.http.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

/**
 * <p>
 * HTTP请求参数校验工具
 * </p>
 *
 * @author albert pi
 * @since 1.0.0
 */

public class ValidationUtils {

	private static Validator validator;

	static {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	public static void validate(Object object, Class<?>... groups) throws ValidationException {
		Set<ConstraintViolation<Object>> constraintViolations = validator.validate(object, groups);
		if (!constraintViolations.isEmpty()) {
			StringBuilder msg = new StringBuilder();
			for (ConstraintViolation<Object> constraint : constraintViolations) {
				msg.append(constraint.getMessage()).append(";");
			}
			msg.setLength(msg.length() - 1);

			throw new ValidationException(msg.toString());
		}
	}
}
