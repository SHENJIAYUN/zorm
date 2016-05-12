package com.zorm.exception;

public class JdbcProxyException extends ZormException {
	public JdbcProxyException(String message, Throwable root) {
		super( message, root );
	}

	public JdbcProxyException(String message) {
		super( message );
	}
}
