package com.zorm.exception;

public class BatchFailedException extends ZormException {
	public BatchFailedException(String s) {
		super( s );
	}

	public BatchFailedException(String string, Throwable root) {
		super( string, root );
	}
}
