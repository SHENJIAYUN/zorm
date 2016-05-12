package com.zorm.exception;

import java.io.Serializable;

public class WrongClassException extends ZormException {

	private final Serializable identifier;
	private final String entityName;

	public WrongClassException(String msg, Serializable identifier, String clazz) {
		super(msg);
		this.identifier = identifier;
		this.entityName = clazz;
	}
	public Serializable getIdentifier() {
		return identifier;
	}

	public String getMessage() {
		return "Object with id: " +
			identifier +
			" was not of the specified subclass: " +
			entityName +
			" (" + super.getMessage() + ")" ;
	}

	public String getEntityName() {
		return entityName;
	}

}