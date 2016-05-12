package com.zorm.service;

import com.zorm.config.Configuration;
import com.zorm.meta.MetadataImplementor;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionFactoryServiceRegistryFactory;
import com.zorm.session.SessionFactoryServiceRegistryImpl;

public class SessionFactoryServiceRegistryFactoryImpl implements SessionFactoryServiceRegistryFactory {
	private final ServiceRegistryImplementor theBasicServiceRegistry;

	public SessionFactoryServiceRegistryFactoryImpl(ServiceRegistryImplementor theBasicServiceRegistry) {
		this.theBasicServiceRegistry = theBasicServiceRegistry;
	}

	@Override
	public SessionFactoryServiceRegistryImpl buildServiceRegistry(
			SessionFactoryImplementor sessionFactory,
			Configuration configuration) {
		return new SessionFactoryServiceRegistryImpl( theBasicServiceRegistry, sessionFactory, configuration );
	}

	@Override
	public SessionFactoryServiceRegistryImpl buildServiceRegistry(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor metadata) {
		return new SessionFactoryServiceRegistryImpl( theBasicServiceRegistry, sessionFactory, metadata );
	}

}
