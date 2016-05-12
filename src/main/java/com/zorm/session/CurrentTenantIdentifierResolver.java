package com.zorm.session;

public interface CurrentTenantIdentifierResolver {
	/**
	 * Resolve the current tenant identifier.
	 * 
	 * @return The current tenant identifier
	 */
	public String resolveCurrentTenantIdentifier();
	
	public boolean validateExistingCurrentSessions();
}
