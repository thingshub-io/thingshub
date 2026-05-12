package io.thingshub.service.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface DataRegion {

	/**
	 * 数据区名称
	 * 
	 * @return
	 */
	String name();

	/**
	 * 数据区初始大小
	 * 
	 * @return
	 */
	long initSize() default 256 * 1024 * 1024L;

	/**
	 * 数据区最大值
	 * 
	 * @return
	 */
	long maxSize() default 2 * 1024 * 1024 * 1024L;

	/**
	 * 数据是否被持久化到本地或外部存储
	 * 
	 * @return
	 */
	boolean persistent() default true;

	/**
	 * 数据是否在每个本地节点缓存
	 * 
	 * @return
	 */
	boolean local() default false;

}