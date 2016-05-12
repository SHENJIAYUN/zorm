package com.zorm.service;

/**
 * 服务启动程序接口
 */
public interface ServiceInitiator <R extends Service>{
	/**
	 * 初始化服务，在serviceRegistry中该种服务都是唯一的
	 *
	 * @return The service role.
	 */
	public Class<R> getServiceInitiated();
}
