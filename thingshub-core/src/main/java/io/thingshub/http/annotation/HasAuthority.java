package io.thingshub.http.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限检查注解
 */
@Documented
@Inherited
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface HasAuthority {

	/**
	 * 权限标识符
	 * 
	 * @return
	 */
	String value();

	/**
	 * 描述
	 */
	String desc() default "";
}
