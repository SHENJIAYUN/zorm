package com.zorm.config;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.criteria.Join;

import com.zorm.annotations.reflection.XClass;
import com.zorm.annotations.reflection.XProperty;
import com.zorm.mapping.KeyValue;
import com.zorm.mapping.PersistentClass;
import com.zorm.mapping.Property;
import com.zorm.mapping.Table;

public interface PropertyHolder {
	String getClassName();

	String getEntityOwnerClassName();

	Table getTable();

	void addProperty(Property prop, XClass declaringClass);

	void addProperty(Property prop, Ejb3Column[] columns, XClass declaringClass);

	KeyValue getIdentifier();

	/**
	 * Return true if this component is or is embedded in a @EmbeddedId
	 */
	boolean isOrWithinEmbeddedId();

	PersistentClass getPersistentClass();

	boolean isComponent();

	boolean isEntity();

	void setParentProperty(String parentProperty);

	String getPath();

	/**
	 * return null if the column is not overridden, or an array of column if true
	 */
	Column[] getOverriddenColumn(String propertyName);

	/**
	 * return null if the column is not overridden, or an array of column if true
	 */
	//JoinColumn[] getOverriddenJoinColumn(String propertyName);

	/**
	 * return
	 *  - null if no join table is present,
	 *  - the join table if not overridden,
	 *  - the overridden join table otherwise
	 */
	//JoinTable getJoinTable(XProperty property);

	String getEntityName();

	//Join addJoin(JoinTable joinTableAnn, boolean noDelayInPkColumnCreation);

	boolean isInIdClass();

	void setInIdClass(Boolean isInIdClass);

	JoinColumn[] getOverriddenJoinColumn(String propertyName);

	JoinTable getJoinTable(XProperty property);
}
