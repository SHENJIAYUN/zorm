package com.zorm.service;

import java.util.Map;

public interface Configurable {
	/**
	 * Configure the service.
	 *
	 * @param configurationValues The configuration properties.
	 */
	public void configure(Map configurationValues);
}
