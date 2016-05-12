package com.zorm.exception;

public class SessionException extends ZormException {
	/**
	 * Constructs a new SessionException with the given message.
	 *
	 * @param message The message indicating the specific problem.
	 */
	public SessionException(String message) {
		super( message );
	}

	/**
	 * Constructs a new SessionException with the given message.
	 *
	 * @param message The message indicating the specific problem.
	 * @param cause An exception which caused this exception to be created.
	 */
	public SessionException(String message, Throwable cause) {
		super( message, cause );
	}
}
