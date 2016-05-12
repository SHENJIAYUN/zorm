package com.zorm.annotations;

public enum PolymorphismType {
	/**
	 * default, this entity is retrieved if any of its super entity is asked
	 */
	IMPLICIT,
	/**
	 * this entity is retrieved only if explicitly asked
	 */
	EXPLICIT
}
