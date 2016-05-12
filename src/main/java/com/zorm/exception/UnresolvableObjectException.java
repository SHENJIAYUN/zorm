package com.zorm.exception;

import java.io.Serializable;

import com.zorm.util.MessageHelper;

public class UnresolvableObjectException extends ZormException{
	private final Serializable identifier;
	private final String entityName;

	public UnresolvableObjectException(Serializable identifier, String clazz) {
		this("No row with the given identifier exists", identifier, clazz);
	}
	UnresolvableObjectException(String message, Serializable identifier, String clazz) {
		super(message);
		this.identifier = identifier;
		this.entityName = clazz;
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

	public static void throwIfNull(Object o, Serializable id, String clazz)
	throws UnresolvableObjectException {
		if (o==null) throw new UnresolvableObjectException(id, clazz);
	}

}
