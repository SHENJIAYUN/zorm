package com.zorm.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.zorm.jdbc.JdbcServicesInitiator;

public class StandardServiceInitiators {
	public static List<BasicServiceInitiator> LIST = buildStandardServiceInitiatorList();

	private static List<BasicServiceInitiator> buildStandardServiceInitiatorList() {
		final List<BasicServiceInitiator> serviceInitiators = new ArrayList<BasicServiceInitiator>();

		//添加各种服务
		serviceInitiators.add( ConfigurationServiceInitiator.INSTANCE );
//		serviceInitiators.add( ImportSqlCommandExtractorInitiator.INSTANCE );
//
//		serviceInitiators.add( JndiServiceInitiator.INSTANCE );
//		serviceInitiators.add( JmxServiceInitiator.INSTANCE );
//
		serviceInitiators.add( PersisterClassResolverInitiator.INSTANCE );
		serviceInitiators.add( PersisterFactoryInitiator.INSTANCE );
//
		serviceInitiators.add( ConnectionProviderInitiator.INSTANCE );
//		serviceInitiators.add( MultiTenantConnectionProviderInitiator.INSTANCE );
		serviceInitiators.add( DialectResolverInitiator.INSTANCE );
		serviceInitiators.add( DialectFactoryInitiator.INSTANCE );
		serviceInitiators.add( BatchBuilderInitiator.INSTANCE );
		serviceInitiators.add( JdbcServicesInitiator.INSTANCE );
//
//		serviceInitiators.add( MutableIdentifierGeneratorFactoryInitiator.INSTANCE);
//
		serviceInitiators.add( JtaPlatformInitiator.INSTANCE );
		serviceInitiators.add( TransactionFactoryInitiator.INSTANCE );
		
//
		serviceInitiators.add( SessionFactoryServiceRegistryFactoryInitiator.INSTANCE );
//
//		serviceInitiators.add( RegionFactoryInitiator.INSTANCE );

		return Collections.unmodifiableList( serviceInitiators );
	}
}
