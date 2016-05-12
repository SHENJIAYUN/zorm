package com.zorm.meta;

import com.zorm.engine.Mapping;
import com.zorm.engine.ResultSetMappingDefinition;
import com.zorm.mapping.IdGenerator;
import com.zorm.service.ServiceRegistry;
import com.zorm.type.TypeResolver;

public interface MetadataImplementor extends Metadata,BindingContext,Mapping{
	public ServiceRegistry getServiceRegistry();

	//public Database getDatabase();

	public TypeResolver getTypeResolver();

	public void addImport(String entityName, String entityName1);

	//public void addEntity(EntityBinding entityBinding);

	//public void addCollection(PluralAttributeBinding collectionBinding);

	//public void addFetchProfile(FetchProfile profile);

	//public void addTypeDefinition(TypeDef typeDef);

	//public void addFilterDefinition(FilterDefinition filterDefinition);

	public void addIdGenerator(IdGenerator generator);

	public void registerIdentifierGenerator(String name, String clazz);

	//public void addNamedNativeQuery(NamedSQLQueryDefinition def);

	//public void addNamedQuery(NamedQueryDefinition def);

	public void addResultSetMapping(ResultSetMappingDefinition resultSetMappingDefinition);

	// todo : this needs to move to AnnotationBindingContext
	public void setGloballyQuotedIdentifiers(boolean b);

	//public MetaAttributeContext getGlobalMetaAttributeContext();
}
