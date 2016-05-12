package com.zorm.exception;

public class TransactionException extends ZormException {

	public TransactionException(String message, Throwable root) {
		super(message,root);
	}

	public TransactionException(String message) {
		super(message);
	}

}