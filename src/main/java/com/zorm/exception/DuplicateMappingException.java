package com.zorm.exception;

public class DuplicateMappingException extends MappingException {
	public static enum Type {
		ENTITY,
		TABLE,
		PROPERTY,
		COLUMN
	}

	private final String name;
	private final String type;

	public DuplicateMappingException(Type type, String name) {
		this( type.name(), name );
	}

	public DuplicateMappingException(String type, String name) {
		this( "Duplicate " + type + " mapping " + name, type, name );
	}

	public DuplicateMappingException(String customMessage, Type type, String name) {
		this( customMessage, type.name(), name );
	}

	public DuplicateMappingException(String customMessage, String type, String name) {
		super( customMessage );
		this.type=type;
		this.name=name;
	}

	public String getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
}
