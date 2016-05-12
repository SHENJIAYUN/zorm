package com.zorm.type;

import com.zorm.dialect.Dialect;

public interface LiteralType<T> {
	/**
	 * Convert the value into a string representation, suitable for embedding in an SQL statement as a
	 * literal.
	 *
	 * @param value The value to convert
	 * @param dialect The SQL dialect
	 *
	 * @return The value's string representation
	 * 
	 * @throws Exception Indicates an issue converting the value to literal string.
	 */
	public String objectToSQLString(T value, Dialect dialect) throws Exception;
}
