package com.zorm.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, METHOD, TYPE})
@Retention(RUNTIME)

/**
 * Define the foreign key name
 */
public @interface ForeignKey {
	/**
	 * Name of the foreign key.  Used in OneToMany, ManyToOne, and OneToOne
	 * relationships.  Used for the owning side in ManyToMany relationships
	 */
	String name();

	/**
	 * Used for the non-owning side of a ManyToMany relationship.  Ignored
	 * in other relationships
	 */
	String inverseName() default "";
}
