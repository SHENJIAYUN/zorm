package com.zorm.service;

import java.util.Map;

import com.zorm.config.AvailableSettings;
import com.zorm.config.Environment;
import com.zorm.service.jta.platform.spi.JtaPlatform;

public class JtaPlatformInitiator implements BasicServiceInitiator<JtaPlatform> {
	public static final JtaPlatformInitiator INSTANCE = new JtaPlatformInitiator();

	@Override
	public Class<JtaPlatform> getServiceInitiated() {
		return JtaPlatform.class;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public JtaPlatform initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object platform = getConfiguredPlatform( configurationValues, registry );
		if ( platform == null ) {
			return new NoJtaPlatform();
		}
		return registry.getService( ConfigurationService.class )
				.cast( JtaPlatform.class, platform );

	}

	private Object getConfiguredPlatform(Map configVales, ServiceRegistryImplementor registry) {
		Object platform = configVales.get( AvailableSettings.JTA_PLATFORM );
		if ( platform == null ) {
			final String transactionManagerLookupImplName = (String) configVales.get( Environment.TRANSACTION_MANAGER_STRATEGY );
			if ( transactionManagerLookupImplName != null ) {
				platform = mapLegacyClasses( transactionManagerLookupImplName, configVales, registry );
			}
		}
		return platform;
	}

	private JtaPlatform mapLegacyClasses(String tmlImplName, Map configVales, ServiceRegistryImplementor registry) {
		if ( tmlImplName == null ) {
			return null;
		}
 
		return null;

//		try {
//			TransactionManagerLookup lookup = (TransactionManagerLookup) registry.getService( ClassLoaderService.class )
//					.classForName( tmlImplName )
//					.newInstance();
//			return new TransactionManagerLookupBridge( lookup, JndiHelper.extractJndiProperties( configVales ) );
//		}
//		catch ( Exception e ) {
//			throw new JtaPlatformException(
//					"Unable to build " + TransactionManagerLookupBridge.class.getName() + " from specified " +
//							TransactionManagerLookup.class.getName() + " implementation: " +
//							tmlImplName
//			);
//		}
	}
}