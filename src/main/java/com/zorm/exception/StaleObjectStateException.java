package com.zorm.exception;

import java.io.Serializable;

import com.zorm.util.MessageHelper;

public class StaleObjectStateException extends StaleStateException {
	private final String entityName;
	private final Serializable identifier;

	public StaleObjectStateException(String persistentClass, Serializable identifier) {
		super("Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect)");
		this.entityName = persistentClass;
		this.identifier = identifier;
	}

	public String getEntityName() {
		return entityName;
	}

	public Serializable getIdentifier() {
		return identifier;
	}

	public String getMessage() {
		return super.getMessage() + ": " +
			MessageHelper.infoString(entityName, identifier);
	}

}
