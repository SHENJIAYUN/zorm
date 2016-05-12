package com.zorm.service;

import java.util.*;

import com.zorm.config.AvailableSettings;
import com.zorm.exception.ServiceException;
import com.zorm.exception.ZormException;
import com.zorm.util.StringHelper;

public class DialectResolverInitiator implements BasicServiceInitiator<DialectResolver> {
	public static final DialectResolverInitiator INSTANCE = new DialectResolverInitiator();

	@Override
	public Class<DialectResolver> getServiceInitiated() {
		return DialectResolver.class;
	}

	@Override
	public DialectResolver initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new DialectResolverSet( determineResolvers( configurationValues, registry ) );
	}

	private List<DialectResolver> determineResolvers(Map configurationValues, ServiceRegistryImplementor registry) {
		final List<DialectResolver> resolvers = new ArrayList<DialectResolver>();

		final String resolverImplNames = (String) configurationValues.get( AvailableSettings.DIALECT_RESOLVERS );

		if ( StringHelper.isNotEmpty( resolverImplNames ) ) {
			final ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
			for ( String resolverImplName : StringHelper.split( ", \n\r\f\t", resolverImplNames ) ) {
				try {
					resolvers.add( (DialectResolver) classLoaderService.classForName( resolverImplName ).newInstance() );
				}
				catch (ZormException e) {
					throw e;
				}
				catch (Exception e) {
					throw new ServiceException( "Unable to instantiate named dialect resolver [" + resolverImplName + "]", e );
				}
			}
		}

		resolvers.add( new StandardDialectResolver() );
		return resolvers;
	}
}