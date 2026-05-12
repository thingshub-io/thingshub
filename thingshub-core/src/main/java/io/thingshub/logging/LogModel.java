package io.thingshub.logging;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LogModel {

	private long timestamp;

	private String thread;

	private String level;

	private String logger;

	private Integer line;

	private String msg;

	private String host;

	private double score;

}
