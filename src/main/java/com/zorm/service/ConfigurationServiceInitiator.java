package com.zorm.service;

import java.util.Map;

public class ConfigurationServiceInitiator implements BasicServiceInitiator<ConfigurationService> {
	public static final ConfigurationServiceInitiator INSTANCE = new ConfigurationServiceInitiator();

	public ConfigurationService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new ConfigurationServiceImpl( configurationValues );
	}

	@Override
	public Class<ConfigurationService> getServiceInitiated() {
		return ConfigurationService.class;
	}
}