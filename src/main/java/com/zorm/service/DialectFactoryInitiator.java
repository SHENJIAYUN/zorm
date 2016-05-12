package com.zorm.service;

import java.util.Map;

import com.zorm.dialect.DialectFactory;
import com.zorm.dialect.DialectFactoryImpl;

public class DialectFactoryInitiator implements BasicServiceInitiator<DialectFactory>{

	public static final DialectFactoryInitiator INSTANCE = new DialectFactoryInitiator();
	
	@Override
	public Class<DialectFactory> getServiceInitiated() {
		return DialectFactory.class;
	}

	@Override
	public DialectFactory initiateService(Map configurationValues,
			ServiceRegistryImplementor registry) {
		return new DialectFactoryImpl();
	}

}
