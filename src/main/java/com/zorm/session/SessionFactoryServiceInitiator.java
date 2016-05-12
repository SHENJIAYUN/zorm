package com.zorm.session;

import com.zorm.config.Configuration;
import com.zorm.meta.MetadataImplementor;
import com.zorm.service.Service;
import com.zorm.service.ServiceInitiator;
import com.zorm.service.ServiceRegistryImplementor;

public interface SessionFactoryServiceInitiator<R extends Service> extends ServiceInitiator<R>{
	/*
	 * 初始化服务
	 */
	public R initiateService(SessionFactoryImplementor sessionFactory, Configuration configuration, ServiceRegistryImplementor registry);

	public R initiateService(SessionFactoryImplementor sessionFactory, MetadataImplementor metadata, ServiceRegistryImplementor registry);
}
