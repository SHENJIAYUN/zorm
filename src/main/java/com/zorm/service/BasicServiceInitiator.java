package com.zorm.service;

import java.util.Map;

public interface BasicServiceInitiator<R extends Service> extends ServiceInitiator<R>{
	/**
	 * Initiates the managed service.
	 *
	 * @param configurationValues The configuration values in effect
	 * @param registry The service registry.  Can be used to locate services needed to fulfill initiation.
	 *
	 * @return The initiated service.
	 */
	public R initiateService(Map configurationValues, ServiceRegistryImplementor registry);
}
