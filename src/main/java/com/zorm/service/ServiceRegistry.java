package com.zorm.service;

/**
 * A registry of services
 * 
 * @author Administrator
 *
 */
public interface ServiceRegistry {
	
  public ServiceRegistry getParentServiceRegistry();
  
  public <R extends Service> R getService(Class<R> serviceRole);
}
