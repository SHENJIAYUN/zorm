package com.zorm.service;

import java.util.Map;

import com.zorm.config.Environment;
import com.zorm.engine.transaction.spi.TransactionImplementor;
import com.zorm.exception.ZormException;
import com.zorm.transaction.JdbcTransactionFactory;

public class TransactionFactoryInitiator<T extends TransactionImplementor> implements BasicServiceInitiator<TransactionFactory> {

	public static final TransactionFactoryInitiator INSTANCE = new TransactionFactoryInitiator();

	@Override
	@SuppressWarnings( {"unchecked"})
	public Class<TransactionFactory> getServiceInitiated() {
		return TransactionFactory.class;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public TransactionFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object strategy = configurationValues.get( Environment.TRANSACTION_STRATEGY );
		if ( TransactionFactory.class.isInstance( strategy ) ) {
			return (TransactionFactory) strategy;
		}

		if ( strategy == null ) {
			return new JdbcTransactionFactory();
		}

		final String strategyClassName = mapLegacyNames( strategy.toString() );

		ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
		try {
			return (TransactionFactory) classLoaderService.classForName( strategyClassName ).newInstance();
		}
		catch ( Exception e ) {
			throw new ZormException( "Unable to instantiate specified TransactionFactory class [" + strategyClassName + "]", e );
		}
	}

	private String mapLegacyNames(String name) {
		if ( "org.hibernate.transaction.JDBCTransactionFactory".equals( name ) ) {
			return JdbcTransactionFactory.class.getName();
		}

		if ( "org.hibernate.transaction.JTATransactionFactory".equals( name ) ) {
			//return JtaTransactionFactory.class.getName();
		}

		if ( "org.hibernate.transaction.CMTTransactionFactory".equals( name ) ) {
			//return CMTTransactionFactory.class.getName();
		}

		return name;
	}
}
