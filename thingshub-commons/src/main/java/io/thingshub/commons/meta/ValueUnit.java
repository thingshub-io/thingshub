package io.thingshub.commons.meta;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValueUnit {

	private String symbol;

	private String title;

}
