package io.thingshub.http.annotation;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.thingshub.ioc.Component;

@Target({ TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Controller {
}