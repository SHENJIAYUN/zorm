package com.zorm.engine;

import com.zorm.exception.MappingException;
import com.zorm.id.IdentifierGeneratorFactory;
import com.zorm.type.Type;

public interface Mapping {
	public IdentifierGeneratorFactory getIdentifierGeneratorFactory();
	public Type getIdentifierType(String className) throws MappingException;
	public String getIdentifierPropertyName(String className) throws MappingException;
	public Type getReferencedPropertyType(String className, String propertyName) throws MappingException;
}
