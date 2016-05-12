package com.zorm.service;

import java.util.Map;

import com.zorm.session.SessionFactoryServiceRegistryFactory;

public class SessionFactoryServiceRegistryFactoryInitiator implements BasicServiceInitiator<SessionFactoryServiceRegistryFactory> {
	public static final SessionFactoryServiceRegistryFactoryInitiator INSTANCE = new SessionFactoryServiceRegistryFactoryInitiator();

	@Override
	public Class<SessionFactoryServiceRegistryFactory> getServiceInitiated() {
		return SessionFactoryServiceRegistryFactory.class;
	}

	@Override
	public SessionFactoryServiceRegistryFactoryImpl initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new SessionFactoryServiceRegistryFactoryImpl( registry );
	}
}