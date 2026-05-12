package io.thingshub.microservice;

import java.io.Serializable;

import lombok.Data;

@Data
public class ServiceInstance implements Serializable {

	private static final long serialVersionUID = 947135411631209013L;

	private String instanceId;

	private String ip;

	private int port;

	private boolean healthy;

	private String serviceName;

}