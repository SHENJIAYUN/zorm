package com.zorm.exception;

public class ClassLoadingException extends ZormException {
	public ClassLoadingException(String string, Throwable root) {
		super( string, root );
	}

	public ClassLoadingException(String s) {
		super( s );
	}
}
