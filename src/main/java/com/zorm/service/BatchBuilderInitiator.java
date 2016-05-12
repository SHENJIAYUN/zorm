package com.zorm.service;

import java.util.Map;

import com.zorm.config.Environment;
import com.zorm.engine.BatchBuilder;
import com.zorm.engine.BatchBuilderImpl;
import com.zorm.exception.ServiceException;
import com.zorm.util.ConfigurationHelper;

public class BatchBuilderInitiator implements BasicServiceInitiator<BatchBuilder> {
	public static final BatchBuilderInitiator INSTANCE = new BatchBuilderInitiator();
	public static final String BUILDER = "zorm.jdbc.batch.builder";

	@Override
	public Class<BatchBuilder> getServiceInitiated() {
		return BatchBuilder.class;
	}

	@Override
	public BatchBuilder initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object builder = configurationValues.get( BUILDER );
		if ( builder == null ) {
			return new BatchBuilderImpl(
					ConfigurationHelper.getInt( Environment.STATEMENT_BATCH_SIZE, configurationValues, 1 )
			);
		}

		if ( BatchBuilder.class.isInstance( builder ) ) {
			return (BatchBuilder) builder;
		}

		final String builderClassName = builder.toString();
		try {
			return (BatchBuilder) registry.getService( ClassLoaderService.class ).classForName( builderClassName ).newInstance();
		}
		catch (Exception e) {
			throw new ServiceException( "Could not build explicit BatchBuilder [" + builderClassName + "]", e );
		}
	}
}
