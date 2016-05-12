package com.zorm.exception;

public class RecoverableException extends AnnotationException {
	public RecoverableException(String msg, Throwable root) {
		super( msg, root );
	}

	public RecoverableException(Throwable root) {
		super( root );
	}

	public RecoverableException(String s) {
		super( s );
	}
}
