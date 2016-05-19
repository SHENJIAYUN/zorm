package com.zorm.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static javax.persistence.FetchType.EAGER;

import javax.persistence.FetchType;

@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface ManyToMany {

	Class targetEntity() default void.class;
	CascadeType[] cascade() default {CascadeType.ALL};
	String mappedBy() default "";
	FetchType fetch() default EAGER;
}
