package io.thingshub.commons.meta;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidateResult {

	private boolean success;

	private String error;

}
