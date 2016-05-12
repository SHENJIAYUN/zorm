package com.zorm.meta;

import java.util.Map;

import com.zorm.config.AccessType;
import com.zorm.config.NamingStrategy;
import com.zorm.mapping.IdGenerator;
import com.zorm.session.SessionFactory;

public interface Metadata {
	/**
	 * Exposes the options used to produce a {@link Metadata} instance.
	 */
	public static interface Options {
		//public MetadataSourceProcessingOrder getMetadataSourceProcessingOrder();
		public NamingStrategy getNamingStrategy();
		//public SharedCacheMode getSharedCacheMode();
		public AccessType getDefaultAccessType();
		public boolean useNewIdentifierGenerators();
        public boolean isGloballyQuotedIdentifiers();
		public String getDefaultSchemaName();
		public String getDefaultCatalogName();
	}

	public Options getOptions();

	//public SessionFactoryBuilder getSessionFactoryBuilder();

	public SessionFactory buildSessionFactory();

	//public Iterable<EntityBinding> getEntityBindings();

	//public EntityBinding getEntityBinding(String entityName);

	/**
	 * Get the "root" entity binding
	 * @param entityName
	 * @return the "root entity binding; simply returns entityBinding if it is the root entity binding
	 */
	//public EntityBinding getRootEntityBinding(String entityName);

	//public Iterable<PluralAttributeBinding> getCollectionBindings();

	//public TypeDef getTypeDefinition(String name);

	//public Iterable<TypeDef> getTypeDefinitions();

	//public Iterable<FilterDefinition> getFilterDefinitions();

	//public Iterable<NamedQueryDefinition> getNamedQueryDefinitions();

	//public Iterable<NamedSQLQueryDefinition> getNamedNativeQueryDefinitions();

	//public Iterable<ResultSetMappingDefinition> getResultSetMappingDefinitions();

	public Iterable<Map.Entry<String, String>> getImports();

	//public Iterable<FetchProfile> getFetchProfiles();

	public IdGenerator getIdGenerator(String name);
}
