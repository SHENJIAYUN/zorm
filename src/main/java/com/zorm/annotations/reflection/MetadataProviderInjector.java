package com.zorm.annotations.reflection;

public interface MetadataProviderInjector {

	MetadataProvider getMetadataProvider();
	
	/**
	 * Defines the metadata provider for a given Reflection Manager
	 */
	void setMetadataProvider(MetadataProvider metadataProvider);
}
