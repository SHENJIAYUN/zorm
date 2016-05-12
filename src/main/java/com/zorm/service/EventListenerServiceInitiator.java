package com.zorm.service;

import com.zorm.config.Configuration;
import com.zorm.event.EventListenerRegistry;
import com.zorm.event.EventListenerRegistryImpl;
import com.zorm.meta.MetadataImplementor;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionFactoryServiceInitiator;

public class EventListenerServiceInitiator implements SessionFactoryServiceInitiator<EventListenerRegistry> {
	public static final EventListenerServiceInitiator INSTANCE = new EventListenerServiceInitiator();

	@Override
	public Class<EventListenerRegistry> getServiceInitiated() {
		return EventListenerRegistry.class;
	}

	@Override
	public EventListenerRegistry initiateService(
			SessionFactoryImplementor sessionFactory,
			Configuration configuration,
			ServiceRegistryImplementor registry) {
		return new EventListenerRegistryImpl();
	}

	@Override
	public EventListenerRegistry initiateService(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor metadata,
			ServiceRegistryImplementor registry) {
		return new EventListenerRegistryImpl();
	}
}