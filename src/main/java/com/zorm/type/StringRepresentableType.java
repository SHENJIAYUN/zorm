package com.zorm.type;

import com.zorm.exception.ZormException;

/**
 * Additional, optional contract for types capable of rendering and consuming their values to/from strings.
 *
 * @author JIA
 */
public interface StringRepresentableType<T> {

	/**
	 * Render the value to the string representation.
	 *
	 * @param value The value to render to string.
	 *
	 * @return The string representation
	 *
	 * @throws Exception Problem rendering
	 */
	public abstract String toString(T value) throws ZormException;
	
	/**
	 * Consume the given string representation back into this types java form.
	 *
	 * @param string The string representation to be consumed.
	 *
	 * @return The java type representation
	 *
	 * @throws Exception Problem consuming
	 */
	public abstract T fromStringValue(String string) throws ZormException;
	
}
