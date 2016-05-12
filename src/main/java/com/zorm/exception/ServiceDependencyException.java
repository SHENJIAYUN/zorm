package com.zorm.exception;

public class ServiceDependencyException extends ZormException {
	public ServiceDependencyException(String string, Throwable root) {
		super( string, root );
	}

	public ServiceDependencyException(String s) {
		super( s );
	}
}
