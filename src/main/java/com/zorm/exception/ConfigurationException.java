package com.zorm.exception;

public class ConfigurationException extends ZormException {
	public ConfigurationException(String string, Throwable root) {
		super( string, root );
	}

	public ConfigurationException(String s) {
		super( s );
	}
}
