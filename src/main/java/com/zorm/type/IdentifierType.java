package com.zorm.type;

public interface IdentifierType<T> extends Type{
	/**
	 * Convert the value from the mapping file to a Java object.
	 *
	 * @param xml the value of <tt>discriminator-value</tt> or <tt>unsaved-value</tt> attribute
	 * @return The converted value of the string representation.
	 *
	 * @throws Exception Indicates a problem converting from the string
	 */
	public T stringToObject(String xml) throws Exception;
}
