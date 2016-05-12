package com.zorm;

import java.util.Map;

import com.zorm.config.Environment;

public enum MultiTenancyStrategy {
   DISCRIMINATOR,
   SCHEMA,
   DATABASE,
   NONE;
   
   public boolean requiresMultiTenantConnectionProvider() {
		return this == DATABASE || this == SCHEMA;
	}
   
   public static MultiTenancyStrategy determineMultiTenancyStrategy(Map properties) {
		final Object strategy = properties.get( Environment.MULTI_TENANT );
		if ( strategy == null ) {
			return MultiTenancyStrategy.NONE;
		}

		if ( MultiTenancyStrategy.class.isInstance( strategy ) ) {
			return (MultiTenancyStrategy) strategy;
		}

		final String strategyName = strategy.toString();
		try {
			return MultiTenancyStrategy.valueOf( strategyName.toUpperCase() );
		}
		catch ( RuntimeException e ) {
			return MultiTenancyStrategy.NONE;
		}
	}
   
}
