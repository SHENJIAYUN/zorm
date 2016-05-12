package com.zorm.exception;

public class UnknownServiceException extends ZormException {
	public final Class serviceRole;

	public UnknownServiceException(Class serviceRole) {
		super( "Unknown service requested [" + serviceRole.getName() + "]" );
		this.serviceRole = serviceRole;
	}

	public Class getServiceRole() {
		return serviceRole;
	}
}