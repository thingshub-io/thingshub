package io.thingshub.ioc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConditionalOnProperty {

	String[] value() default {};

	String prefix() default "";

	String[] name() default {};

	String havingValue() default "";

	boolean matchIfMissing() default false;

}