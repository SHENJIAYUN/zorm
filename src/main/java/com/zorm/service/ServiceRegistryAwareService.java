package com.zorm.service;

public interface ServiceRegistryAwareService {
	/**
	 * Callback to inject the registry.
	 *
	 * @param serviceRegistry The registry
	 */
	public void injectServices(ServiceRegistryImplementor serviceRegistry);
}
