package com.zorm.service;

public interface ServiceRegistryImplementor extends ServiceRegistry{
	public <R extends Service> ServiceBinding<R> locateServiceBinding(Class<R> serviceRole);
    
	/*
	 * Release resources
	 */
	public void destroy();
}
