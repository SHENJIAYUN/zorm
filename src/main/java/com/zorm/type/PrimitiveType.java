package com.zorm.type;

import java.io.Serializable;

public interface PrimitiveType<T> extends LiteralType<T>{
	/**
	 * Retrieve the primitive counterpart to the wrapper type identified by
	 * {@link org.hibernate.type.Type#getReturnedClass()}.
	 *
	 * @return The primitive Java type.
	 */
	public abstract Class getPrimitiveClass();
	
	/**
	 * Retrieve the string representation of the given value.
	 *
	 * @param value The value to be stringified.
	 *
	 * @return The string representation
	 */
	public String toString(T value);
	
	/**
	 * Get this type's default value.
	 *
	 * @return The default value.
	 */
	public abstract Serializable getDefaultValue();
}
