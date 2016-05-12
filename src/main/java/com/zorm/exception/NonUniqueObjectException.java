package com.zorm.exception;

import java.io.Serializable;

import com.zorm.util.MessageHelper;

public class NonUniqueObjectException extends ZormException {
	private final Serializable identifier;
	private final String entityName;

	public NonUniqueObjectException(String message, Serializable id, String clazz) {
		super(message);
		this.entityName = clazz;
		this.identifier = id;
	}

	public NonUniqueObjectException(Serializable id, String clazz) {
		this("a different object with the same identifier value was already associated with the session", id, clazz);
	}

	public Serializable getIdentifier() {
		return identifier;
	}

	public String getMessage() {
		return super.getMessage() + ": " +
			MessageHelper.infoString(entityName, identifier);
	}

	public String getEntityName() {
		return entityName;
	}

}
