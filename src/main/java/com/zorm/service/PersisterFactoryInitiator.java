package com.zorm.service;

import java.util.Map;

import com.zorm.exception.ServiceException;
import com.zorm.persister.PersisterFactory;
import com.zorm.persister.PersisterFactoryImpl;

public class PersisterFactoryInitiator implements BasicServiceInitiator<PersisterFactory> {
	public static final PersisterFactoryInitiator INSTANCE = new PersisterFactoryInitiator();

	public static final String IMPL_NAME = "hibernate.persister.factory";

	@Override
	public Class<PersisterFactory> getServiceInitiated() {
		return PersisterFactory.class;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public PersisterFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object customImpl = configurationValues.get( IMPL_NAME );
		if ( customImpl == null ) {
			return new PersisterFactoryImpl();
		}

		if ( PersisterFactory.class.isInstance( customImpl ) ) {
			return (PersisterFactory) customImpl;
		}

		final Class<? extends PersisterFactory> customImplClass = Class.class.isInstance( customImpl )
				? ( Class<? extends PersisterFactory> ) customImpl
				: locate( registry, customImpl.toString() );
		try {
			return customImplClass.newInstance();
		}
		catch (Exception e) {
			throw new ServiceException( "Could not initialize custom PersisterFactory impl [" + customImplClass.getName() + "]", e );
		}
	}

	private Class<? extends PersisterFactory> locate(ServiceRegistryImplementor registry, String className) {
		return registry.getService( ClassLoaderService.class ).classForName( className );
	}
}
