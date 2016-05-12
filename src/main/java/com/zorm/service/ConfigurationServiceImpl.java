package com.zorm.service;

import java.util.Collections;
import java.util.Map;

import com.zorm.exception.ClassLoadingException;

public class ConfigurationServiceImpl implements ConfigurationService, ServiceRegistryAwareService {
	private final Map settings;
	private ServiceRegistryImplementor serviceRegistry;

	public ConfigurationServiceImpl(Map settings) {
		this.settings = Collections.unmodifiableMap( settings );
	}

	@Override
	public Map getSettings() {
		return settings;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public <T> T getSetting(String name, Converter<T> converter) {
		return getSetting( name, converter, null );
	}

	@Override
	public <T> T getSetting(String name, Converter<T> converter, T defaultValue) {
		final Object value = settings.get( name );
		if ( value == null ) {
			return defaultValue;
		}

		return converter.convert( value );
	}
	@Override
	public <T> T getSetting(String name, Class<T> expected, T defaultValue) {
		Object value = settings.get( name );
		T target = cast( expected, value );
		return target !=null ? target : defaultValue;
	}
	@Override
	public <T> T cast(Class<T> expected, Object candidate){
		if(candidate == null) return null;
		if ( expected.isInstance( candidate ) ) {
			return (T) candidate;
		}
		Class<T> target;
		if ( Class.class.isInstance( candidate ) ) {
			target = Class.class.cast( candidate );
		}
		else {
			try {
				target = serviceRegistry.getService( ClassLoaderService.class ).classForName( candidate.toString() );
			}
			catch ( ClassLoadingException e ) {
				target = null;
			}
		}
		if ( target != null ) {
			try {
				return target.newInstance();
			}
			catch ( Exception e ) {
			}
		}
		return null;
	}


}

