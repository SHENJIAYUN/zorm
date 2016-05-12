package com.zorm.session;

import com.zorm.config.Configuration;
import com.zorm.meta.MetadataImplementor;
import com.zorm.service.AbstractServiceRegistryImpl;
import com.zorm.service.Service;
import com.zorm.service.ServiceBinding;
import com.zorm.service.ServiceInitiator;
import com.zorm.service.ServiceRegistryImplementor;
import com.zorm.service.StandardSessionFactoryServiceInitiators;

public class SessionFactoryServiceRegistryImpl extends AbstractServiceRegistryImpl implements SessionFactoryServiceRegistry{
	private final Configuration configuration;
	private final MetadataImplementor metadata;
	private final SessionFactoryImplementor sessionFactory;
	
	@SuppressWarnings( {"unchecked"})
	public SessionFactoryServiceRegistryImpl(
			ServiceRegistryImplementor parent,
			SessionFactoryImplementor sessionFactory,
			Configuration configuration) {
		super( parent );

		this.sessionFactory = sessionFactory;
		this.configuration = configuration;
		this.metadata = null;

		// for now, just use the standard initiator list
		for ( SessionFactoryServiceInitiator initiator : StandardSessionFactoryServiceInitiators.LIST ) {
			// create the bindings up front to help identify to which registry services belong
			createServiceBinding( initiator );
		}
	}
	
	@SuppressWarnings( {"unchecked"})
	public SessionFactoryServiceRegistryImpl(
			ServiceRegistryImplementor parent,
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor metadata) {
		super( parent );

		this.sessionFactory = sessionFactory;
		this.configuration = null;
		this.metadata = metadata;

		// for now, just use the standard initiator list
		for ( SessionFactoryServiceInitiator initiator : StandardSessionFactoryServiceInitiators.LIST ) {
			// create the bindings up front to help identify to which registry services belong
			createServiceBinding( initiator );
		}
	}
	
	@Override
	public <R extends Service> R initiateService(ServiceInitiator<R> serviceInitiator) {
		// todo : add check/error for unexpected initiator types?
		SessionFactoryServiceInitiator<R> sessionFactoryServiceInitiator =
				(SessionFactoryServiceInitiator<R>) serviceInitiator;
		if ( metadata != null ) {
			return sessionFactoryServiceInitiator.initiateService( sessionFactory, metadata, this );
		}
		else if ( configuration != null ) {
			return sessionFactoryServiceInitiator.initiateService( sessionFactory, configuration, this );
		}
		else {
			throw new IllegalStateException( "Both metadata and configuration are null." );
		}
	}

	@Override
	public <R extends Service> void configureService(ServiceBinding<R> serviceBinding) {
	}
}
