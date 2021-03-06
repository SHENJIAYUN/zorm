package com.zorm.config;

import com.zorm.annotations.reflection.XClass;
import com.zorm.annotations.reflection.XProperty;
import com.zorm.exception.MappingException;

public interface PropertyData {
	/**
	 * @return default member access (whether field or property)
	 * @throws MappingException No getter or field found or wrong JavaBean spec usage
	 */
	AccessType getDefaultAccess();
	
	/**
	 * @return property name
	 * @throws MappingException No getter or field found or wrong JavaBean spec usage
	 */
	String getPropertyName() throws MappingException;
	
	/**
	 * Returns the returned class itself or the element type if an array
	 */
	XClass getClassOrElement() throws MappingException;
	
	/**
	 * Return the class itself
	 */
	XClass getPropertyClass() throws MappingException;
	
	/**
	 * Returns the returned class name itself or the element type if an array
	 */
	String getClassOrElementName() throws MappingException;
	
	/**
	 * Returns the returned class name itself
	 */
	String getTypeName() throws MappingException;
	
	/**
	 * Return the mapping property
	 */
	XProperty getProperty();
	
	/**
	 * Return the Class the property is declared on
	 * If the property is declared on a @MappedSuperclass,
	 * this class will be different than the PersistentClass's class
	 */
	XClass getDeclaringClass();
}
