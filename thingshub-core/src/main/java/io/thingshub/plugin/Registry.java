package io.thingshub.plugin;

import java.util.List;

import io.thingshub.microservice.ServiceInstance;

public interface Registry {

	public void register(ServiceInstance serviceInstance);

	public List<ServiceInstance> getAllInstances(String serviceName);

	public ServiceInstance selectHealthInstance(String serviceName);

}