package com.zorm.exception;

public class UnknownPersisterException extends ZormException {
	public UnknownPersisterException(String s) {
		super( s );
	}

	public UnknownPersisterException(String string, Throwable root) {
		super( string, root );
	}
}