package io.thingshub.service.base;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {

	/**
	 * 实体对应的数据库表名
	 */
	String value() default "";

	/**
	 * schema
	 */
	String schema() default "";

}