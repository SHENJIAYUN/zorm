package com.zorm.service;

public class ProvidedService<R> {
  private final Class<R> serviceRole;
  private final R service;
  
  public ProvidedService(Class<R> serviceRole,R service){
	  this.serviceRole = serviceRole;
	  this.service = service;
  }
  
  public Class<R> getServiceRole() {
		return serviceRole;
	}

	public R getService() {
		return service;
	}
}
