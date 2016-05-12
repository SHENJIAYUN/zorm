package com.zorm.persister;

import com.zorm.exception.MappingException;
import com.zorm.exception.QueryException;
import com.zorm.persister.entity.PropertyMapping;
import com.zorm.type.Type;
import com.zorm.util.StringHelper;

public class ElementPropertyMapping implements PropertyMapping {

	private final String[] elementColumns;
	private final Type type;

	public ElementPropertyMapping(String[] elementColumns, Type type)
	throws MappingException {
		this.elementColumns = elementColumns;
		this.type = type;
	}

	public Type toType(String propertyName) throws QueryException {
		if ( propertyName==null || "id".equals(propertyName) ) {
			return type;
		}
		else {
			throw new QueryException("cannot dereference scalar collection element: " + propertyName);
		}
	}

	public String[] toColumns(String alias, String propertyName) throws QueryException {
		if (propertyName==null || "id".equals(propertyName) ) {
			return StringHelper.qualify( alias, elementColumns );
		}
		else {
			throw new QueryException("cannot dereference scalar collection element: " + propertyName);
		}
	}

	/**
	 * Given a property path, return the corresponding column name(s).
	 */
	public String[] toColumns(String propertyName) throws QueryException, UnsupportedOperationException {
		throw new UnsupportedOperationException( "References to collections must be define a SQL alias" );
	}

	public Type getType() {
		return type;
	}

}
