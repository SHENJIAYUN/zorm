package com.zorm.service;

import java.util.List;
import java.util.Map;

public class StandardServiceRegistryImpl extends AbstractServiceRegistryImpl implements ServiceRegistry {

	private final Map configurationValues;
	
	public StandardServiceRegistryImpl(
			BootstrapServiceRegistry bootstrapServiceRegistry, List<BasicServiceInitiator> serviceInitiators,
			List<ProvidedService> providedServices, Map<?, ?> configurationValues) {

		super(bootstrapServiceRegistry);
		
		this.configurationValues = configurationValues;

		for ( ServiceInitiator initiator : serviceInitiators ) {
			createServiceBinding( initiator );
		}

		for ( ProvidedService providedService : providedServices ) {
			createServiceBinding( providedService );
		}
	}

	@Override
	public ServiceRegistry getParentServiceRegistry() {
		return null;
	}

	@Override
	public <R extends Service> R initiateService(ServiceInitiator<R> serviceInitiator) {
		return ( (BasicServiceInitiator<R>) serviceInitiator ).initiateService( configurationValues, this );
	}
	
	@Override
	public <R extends Service> void configureService(ServiceBinding<R> serviceBinding) {
		if ( Configurable.class.isInstance( serviceBinding.getService() ) ) {
			( (Configurable) serviceBinding.getService() ).configure( configurationValues );
		}
	}

}
