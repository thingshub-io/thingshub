package io.thingshub.http.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.thingshub.http.HttpMethod;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {

	String name() default "";

	String[] value() default {};

	String[] path() default {};

	HttpMethod[] method() default { HttpMethod.GET };

//	String[] params() default {};

	String[] headers() default {};

	String[] consumes() default { "application/json;charset=UTF-8" };

	String[] produces() default { "application/json;charset=UTF-8" };

}