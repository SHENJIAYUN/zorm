package com.zorm.meta;

import com.zorm.config.NamingStrategy;
import com.zorm.service.ServiceRegistry;
import com.zorm.type.Type;

public interface BindingContext {
	public ServiceRegistry getServiceRegistry();

	public NamingStrategy getNamingStrategy();

	//public MappingDefaults getMappingDefaults();

	public MetadataImplementor getMetadataImplementor();

	public <T> Class<T> locateClassByName(String name);

	public Type makeJavaType(String className);

	public boolean isGloballyQuotedIdentifiers();

	//public ValueHolder<Class<?>> makeClassReference(String className);

	public String qualifyClassName(String name);
}

