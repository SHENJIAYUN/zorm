package com.zorm.config;

public enum AnnotatedClassType {
	/**
	 * has no revelent top level annotation
	 */
	NONE,
	/**
	 * has @Entity annotation
	 */
	ENTITY,
	/**
	 * has a @Embeddable annotation
	 */
	EMBEDDABLE,
	/**
	 * has @EmbeddedSuperclass annotation
	 */
	EMBEDDABLE_SUPERCLASS
}
