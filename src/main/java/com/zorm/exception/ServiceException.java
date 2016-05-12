package com.zorm.exception;

public class ServiceException extends ZormException {
	public ServiceException(String message, Throwable root) {
		super( message, root );
	}

	public ServiceException(String message) {
		super( message );
	}
}
