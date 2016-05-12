package com.zorm.mapping;

import com.zorm.property.PropertyAccessor;

public class Backref extends Property {
	private String collectionRole;
	private String entityName;

	/**
	 * {@inheritDoc}
	 */
	public boolean isBackRef() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isSynthetic() {
		return true;
	}

	public String getCollectionRole() {
		return collectionRole;
	}

	public void setCollectionRole(String collectionRole) {
		this.collectionRole = collectionRole;
	}

	public boolean isBasicPropertyAccessor() {
		return false;
	}

//	public PropertyAccessor getPropertyAccessor(Class clazz) {
//		return new BackrefPropertyAccessor(collectionRole, entityName);
//	}
	
	public String getEntityName() {
		return entityName;
	}
	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}
}
