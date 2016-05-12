package com.zorm.exception;

public class ResourceClosedException extends ZormException {
	public ResourceClosedException(String s) {
		super( s );
	}

	public ResourceClosedException(String string, Throwable root) {
		super( string, root );
	}
}
