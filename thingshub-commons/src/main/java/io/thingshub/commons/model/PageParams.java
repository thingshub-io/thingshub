package io.thingshub.commons.model;

import lombok.Data;

@Data
public abstract class PageParams {

	protected Integer page = 1;

	protected Integer size = 10;

}
