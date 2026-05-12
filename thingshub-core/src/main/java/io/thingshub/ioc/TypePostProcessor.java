package io.thingshub.ioc;

public interface TypePostProcessor<T> {

	void process(T instance) throws Exception;

}
