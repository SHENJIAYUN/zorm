package com.zorm.exception;

import com.zorm.util.StringHelper;

public class PropertyValueException extends ZormException {

	private final String entityName;
	private final String propertyName;

	public PropertyValueException(String s, String entityName, String propertyName) {
		super(s);
		this.entityName = entityName;
		this.propertyName = propertyName;
	}

	public String getEntityName() {
		return entityName;
	}

	public String getPropertyName() {
		return propertyName;
	}

	@Override
    public String getMessage() {
		return super.getMessage() + ": " +
			StringHelper.qualify(entityName, propertyName);
	}

	/**
	 * Return a well formed property path.
	 * Basicaly, it will return parent.child
	 *
	 * @param parent parent in path
	 * @param child child in path
	 * @return parent-child path
	 */
	public static String buildPropertyPath(String parent, String child) {
		return new StringBuilder(parent).append('.').append(child).toString();
	}
}
