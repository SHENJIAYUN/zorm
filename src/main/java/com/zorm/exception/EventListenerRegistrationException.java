package com.zorm.exception;

public class EventListenerRegistrationException extends ZormException {
	public EventListenerRegistrationException(String s) {
		super( s );
	}

	public EventListenerRegistrationException(String string, Throwable root) {
		super( string, root );
	}
}
