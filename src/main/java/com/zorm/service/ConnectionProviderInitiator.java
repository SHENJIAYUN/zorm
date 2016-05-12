package com.zorm.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.zorm.MultiTenancyStrategy;
import com.zorm.config.Environment;
import com.zorm.jdbc.ConnectionProvider;
import com.zorm.jdbc.DriverManagerConnectionProviderImpl;

public class ConnectionProviderInitiator implements BasicServiceInitiator<ConnectionProvider>{
	public static final ConnectionProviderInitiator INSTANCE = new ConnectionProviderInitiator();
	
	public static final String C3P0_PROVIDER_CLASS_NAME =
			"org.hibernate.service.jdbc.connections.internal.C3P0ConnectionProvider";

	public static final String PROXOOL_PROVIDER_CLASS_NAME =
			"org.hibernate.service.jdbc.connections.internal.ProxoolConnectionProvider";

	public static final String INJECTION_DATA = "hibernate.connection_provider.injection_data";
	
	private static final Map<String,String> LEGACY_CONNECTION_PROVIDER_MAPPING;

	static {
		LEGACY_CONNECTION_PROVIDER_MAPPING = new HashMap<String,String>( 5 );

//		LEGACY_CONNECTION_PROVIDER_MAPPING.put(
//				"org.hibernate.connection.DatasourceConnectionProvider",
//				DatasourceConnectionProviderImpl.class.getName()
//		);
//		LEGACY_CONNECTION_PROVIDER_MAPPING.put(
//				"org.hibernate.connection.DriverManagerConnectionProvider",
//				DriverManagerConnectionProviderImpl.class.getName()
//		);
//		LEGACY_CONNECTION_PROVIDER_MAPPING.put(
//				"org.hibernate.connection.UserSuppliedConnectionProvider",
//				UserSuppliedConnectionProviderImpl.class.getName()
//		);
		LEGACY_CONNECTION_PROVIDER_MAPPING.put(
				"org.hibernate.connection.C3P0ConnectionProvider",
				C3P0_PROVIDER_CLASS_NAME
		);
		LEGACY_CONNECTION_PROVIDER_MAPPING.put(
				"org.hibernate.connection.ProxoolConnectionProvider",
				PROXOOL_PROVIDER_CLASS_NAME
		);
	}

	@Override
	public Class<ConnectionProvider> getServiceInitiated() {
		return ConnectionProvider.class;
	}

	@Override
	public ConnectionProvider initiateService(Map configurationValues,
			ServiceRegistryImplementor registry) {
		final MultiTenancyStrategy strategy = MultiTenancyStrategy.determineMultiTenancyStrategy(  configurationValues );
		if ( strategy == MultiTenancyStrategy.DATABASE || strategy == MultiTenancyStrategy.SCHEMA ) {
			// nothing to do, but given the separate hierarchies have to handle this here.
			return null;
		}
		final ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
		
		ConnectionProvider connectionProvider = null;
		if ( connectionProvider == null ) {
			if ( configurationValues.get( Environment.URL ) != null ) {
				connectionProvider = new DriverManagerConnectionProviderImpl();
			}
		}
		return connectionProvider;
	}

	public static Properties getConnectionProperties(Map<?,?> properties) {
        Properties result = new Properties();
        for(Map.Entry entry : properties.entrySet()){
        	if(!(String.class.isInstance(entry.getKey())) || !String.class.isInstance(entry.getValue())){
        		continue;
        	}
        	final String key = (String) entry.getKey();
        	final String value = (String) entry.getValue();
        	if(key.startsWith(Environment.CONNECTION_PREFIX)){
        		if(SPECIAL_PROPERTIES.contains(key)){
        			if(Environment.USER.equals(key)){
        				result.setProperty("user", value);
        			}
        		}
        		else{
        			result.setProperty(key.substring(Environment.CONNECTION_PREFIX.length() + 1 ), value);
        		}
        	}
        }
		return result;
	}

	private static final Set<String> SPECIAL_PROPERTIES;

	static {
		SPECIAL_PROPERTIES = new HashSet<String>();
		SPECIAL_PROPERTIES.add( Environment.URL );
		SPECIAL_PROPERTIES.add( Environment.POOL_SIZE );
		SPECIAL_PROPERTIES.add( Environment.ISOLATION );
		SPECIAL_PROPERTIES.add( Environment.DRIVER );
		SPECIAL_PROPERTIES.add( Environment.USER );

	}
}
