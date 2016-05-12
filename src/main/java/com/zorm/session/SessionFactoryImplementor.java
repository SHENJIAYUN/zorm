package com.zorm.session;

import java.util.Map;
import java.util.Properties;

import com.zorm.CustomEntityDirtinessStrategy;
import com.zorm.config.Settings;
import com.zorm.dialect.Dialect;
import com.zorm.dialect.function.SQLFunctionRegistry;
import com.zorm.engine.Mapping;
import com.zorm.exception.MappingException;
import com.zorm.id.IdentifierGenerator;
import com.zorm.jdbc.JdbcServices;
import com.zorm.jdbc.SqlExceptionHelper;
import com.zorm.meta.ClassMetadata;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.query.QueryPlanCache;
import com.zorm.service.ServiceRegistryImplementor;
import com.zorm.stat.StatisticsImplementor;
import com.zorm.type.TypeResolver;

public interface SessionFactoryImplementor extends Mapping,SessionFactory{

	public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver();

	public EntityPersister getEntityPersister(String entityName) throws MappingException;

	public TypeResolver getTypeResolver();

	public Settings getSettings();

	public ServiceRegistryImplementor getServiceRegistry();

	public Dialect getDialect();

	public SQLFunctionRegistry getSqlFunctionRegistry();

	public IdentifierGenerator getIdentifierGenerator(String rootName);

	public Map<String,ClassMetadata> getAllClassMetadata();

	public JdbcServices getJdbcServices();

	public StatisticsImplementor getStatisticsImplementor();

	public SqlExceptionHelper getSQLExceptionHelper();

	public  CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy();

	public String getImportedClassName(String token);

	public String[] getImplementors(String className) throws MappingException;

	public QueryPlanCache getQueryPlanCache();

	public Properties getProperties();

	public CollectionPersister getCollectionPersister(String role);
}
