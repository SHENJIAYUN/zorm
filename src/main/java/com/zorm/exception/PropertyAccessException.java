package com.zorm.exception;

import com.zorm.util.StringHelper;

public class PropertyAccessException extends ZormException {

	private final Class persistentClass;
	private final String propertyName;
	private final boolean wasSetter;

	public PropertyAccessException(Throwable root, String s, boolean wasSetter, Class persistentClass, String propertyName) {
		super(s, root);
		this.persistentClass = persistentClass;
		this.wasSetter = wasSetter;
		this.propertyName = propertyName;
	}

	public Class getPersistentClass() {
		return persistentClass;
	}

	public String getPropertyName() {
		return propertyName;
	}

	@Override
    public String getMessage() {
		return super.getMessage() +
		( wasSetter ? " setter of " : " getter of ") +
		StringHelper.qualify( persistentClass.getName(), propertyName );
	}
}

