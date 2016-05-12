package com.zorm.session;

import com.zorm.config.Configuration;
import com.zorm.meta.MetadataImplementor;
import com.zorm.service.Service;

public interface SessionFactoryServiceRegistryFactory extends Service{
	public SessionFactoryServiceRegistryImpl buildServiceRegistry(
			SessionFactoryImplementor sessionFactory,
			Configuration configuration);
	
	public SessionFactoryServiceRegistryImpl buildServiceRegistry(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor metadata);
}
