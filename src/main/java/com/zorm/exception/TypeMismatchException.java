package com.zorm.exception;

public class TypeMismatchException extends ZormException {
	public TypeMismatchException(Throwable root) {
		super( root );
	}

	public TypeMismatchException(String s) {
		super( s );
	}

	public TypeMismatchException(String string, Throwable root) {
		super( string, root );
	}
}
