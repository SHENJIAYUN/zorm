package com.zorm.exception;

public class IdentifierGenerationException extends ZormException {
	public IdentifierGenerationException(String msg) {
		super(msg);
	}

	public IdentifierGenerationException(String msg, Throwable t) {
		super(msg, t);
	}
}
