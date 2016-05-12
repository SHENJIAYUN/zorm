package com.zorm.persister.entity;

import com.zorm.exception.QueryException;
import com.zorm.type.Type;

public interface PropertyMapping {
	// TODO: It would be really, really nice to use this to also model components!
		/**
		 * Given a component path expression, get the type of the property
		 */
		public Type toType(String propertyName) throws QueryException;
		/**
		 * Given a query alias and a property path, return the qualified
		 * column name
		 */
		public String[] toColumns(String alias, String propertyName) throws QueryException;
		/**
		 * Given a property path, return the corresponding column name(s).
		 */
		public String[] toColumns(String propertyName) throws QueryException, UnsupportedOperationException;
		/**
		 * Get the type of the thing containing the properties
		 */
		public Type getType();
}
