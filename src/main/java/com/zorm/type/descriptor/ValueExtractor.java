package com.zorm.type.descriptor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Contract for extracting a value from a {@link ResultSet}.
 *
 * @author JIA
 */
public interface ValueExtractor<X> {
	/**
	 * Extract value from result set
	 *
	 * @param rs The result set from which to extract the value
	 * @param name The name by which to extract the value from the result set
	 * @param options The options
	 *
	 * @return The extracted value
	 *
	 * @throws SQLException Indicates a JDBC error occurred.
	 */
	public X extract(ResultSet rs, String name, WrapperOptions options) throws SQLException;
}
