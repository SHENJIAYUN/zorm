package com.zorm.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 服务绑定类
 * @author JIA
 *
 */
public final class ServiceBinding<R extends Service> {
  private static final Log log = LogFactory.getLog(ServiceBinding.class);
  
  public static interface ServiceLifecycleOwner{
	  /**
	   * 初始化服务
	   * @param serviceInitiator
	   * @return R extends Service
	   */
	  public <R extends Service> R initiateService(ServiceInitiator<R> serviceInitiator);
	  /**
	   * 配置服务
	   * @param binding
	   */
	  public  <R extends Service> void configureService(ServiceBinding<R> binding);
	  /**
	   * 为服务注入依赖
	   * @param binding
	   */
	  public <R extends Service> void injectDependencies(ServiceBinding<R> binding);
	  /**
	   * 启动服务
	   * @param binding
	   */
	  public <R extends Service> void startService(ServiceBinding<R> binding);
	  /**
	   * 中断服务
	   * @param binding
	   */
	  public <R extends Service> void stopService(ServiceBinding<R> binding);
  }
  
  //服务生命周期
  private final ServiceLifecycleOwner lifecycleOwner;
  private final Class<R> serviceRole;
  private final ServiceInitiator<R> serviceInitiator;
  private R service;
  
  public ServiceBinding(ServiceLifecycleOwner lifecycleOwner, ServiceInitiator<R> serviceInitiator) {
		this.lifecycleOwner = lifecycleOwner;
		this.serviceRole = serviceInitiator.getServiceInitiated();
		this.serviceInitiator = serviceInitiator;
	}
  
  public ServiceBinding(ServiceLifecycleOwner lifecycleOwner,Class<R> serviceRole,R service){
	  this.lifecycleOwner = lifecycleOwner;
	  this.serviceRole = serviceRole;
	  this.serviceInitiator = null;
	  this.service = service;
  }
  
  public ServiceLifecycleOwner getLifecycleOwner() {
		return lifecycleOwner;
	}

	public Class<R> getServiceRole() {
		return serviceRole;
	}

	public ServiceInitiator<R> getServiceInitiator() {
		return serviceInitiator;
	}

	public R getService() {
		return service;
	}
	
	public void setService(R service){
		if(this.service!=null){
			log.debug("Overriding existing service binding [" + serviceRole.getName() + "]");
		}
		this.service = service;
	}
}
