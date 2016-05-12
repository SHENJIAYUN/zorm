package com.zorm.exception;

public class UnknownUnwrapTypeException extends ZormException {
	public UnknownUnwrapTypeException(Class unwrapType) {
		super( "Cannot unwrap to requested type [" + unwrapType.getName() + "]" );
	}

	public UnknownUnwrapTypeException(Class unwrapType, Throwable root) {
		this( unwrapType );
		super.initCause( root );
	}
}