package com.zorm.config;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.ConnectionReleaseMode;
import com.zorm.MultiTenancyStrategy;
import com.zorm.entity.EntityMode;
import com.zorm.exception.ZormException;
import com.zorm.jdbc.ExtractedDatabaseMetaData;
import com.zorm.jdbc.JdbcServices;
import com.zorm.query.QueryTranslatorFactory;
import com.zorm.service.ClassLoaderService;
import com.zorm.service.JtaPlatform;
import com.zorm.service.ServiceRegistry;
import com.zorm.service.TransactionFactory;
import com.zorm.util.ConfigurationHelper;
import com.zorm.util.StringHelper;

public class SettingsFactory implements Serializable{

	private static final long serialVersionUID = -5107587458735751860L;
    private static final Log log = LogFactory.getLog(SettingsFactory.class);
	
	public SettingsFactory(){}

	public Settings buildSettings(Properties props,
			ServiceRegistry serviceRegistry) {
        final JdbcServices jdbcServices = serviceRegistry.getService(JdbcServices.class);
		Settings settings = new Settings();
        
		//设置SessionFactory名
		String sessionFactoryName = props.getProperty(AvailableSettings.SESSION_FACTORY_NAME);
        settings.setSessionFactoryName(sessionFactoryName);
		
		//JDBC连接设置
		
		//获取数据库连接元数据
		ExtractedDatabaseMetaData meta = jdbcServices.getExtractedMetaDataSupport();
		
		settings.setDataDefinitionImplicitCommit( meta.doesDataDefinitionCauseTransactionCommit() );
		settings.setDataDefinitionInTransactionSupported( meta.supportsDataDefinitionInTransaction() );
		
		//使用dialect默认配置
		final Properties properties = new Properties();
		properties.putAll( jdbcServices.getDialect().getDefaultProperties() );
		properties.putAll( props );
		
		//事务设置
		settings.setJtaPlatform( serviceRegistry.getService( JtaPlatform.class ) );

		boolean flushBeforeCompletion = ConfigurationHelper.getBoolean(AvailableSettings.FLUSH_BEFORE_COMPLETION, properties);
		settings.setFlushBeforeCompletionEnabled(flushBeforeCompletion);
		
		boolean autoCloseSession = ConfigurationHelper.getBoolean(AvailableSettings.AUTO_CLOSE_SESSION, properties);
		settings.setAutoCloseSessionEnabled(autoCloseSession);
		
		//JDBC连接设置
		int batchSize = ConfigurationHelper.getInt(AvailableSettings.STATEMENT_BATCH_SIZE, properties, 0);
		if ( !meta.supportsBatchUpdates() ) {
			batchSize = 0;
		}
		
		boolean jdbcBatchVersionedData = ConfigurationHelper.getBoolean(AvailableSettings.BATCH_VERSIONED_DATA, properties, false);
		settings.setJdbcBatchVersionedData(jdbcBatchVersionedData);
		
		boolean useScrollableResultSets = ConfigurationHelper.getBoolean(
				AvailableSettings.USE_SCROLLABLE_RESULTSET,
				properties,
				meta.supportsScrollableResults()
		);
		settings.setScrollableResultSetsEnabled(useScrollableResultSets);
		
		boolean wrapResultSets = ConfigurationHelper.getBoolean(AvailableSettings.WRAP_RESULT_SETS, properties, false);
		settings.setWrapResultSetsEnabled(wrapResultSets);
		
		boolean useGetGeneratedKeys = ConfigurationHelper.getBoolean(AvailableSettings.USE_GET_GENERATED_KEYS, properties, meta.supportsGetGeneratedKeys());
		settings.setGetGeneratedKeysEnabled(useGetGeneratedKeys);
		
		Integer statementFetchSize = ConfigurationHelper.getInteger(AvailableSettings.STATEMENT_FETCH_SIZE, properties);
		settings.setJdbcFetchSize(statementFetchSize);
		
		MultiTenancyStrategy multiTenancyStrategy = MultiTenancyStrategy.determineMultiTenancyStrategy( properties );
		settings.setMultiTenancyStrategy( multiTenancyStrategy );

		String releaseModeName = ConfigurationHelper.getString( AvailableSettings.RELEASE_CONNECTIONS, properties, "auto" );
		ConnectionReleaseMode releaseMode;
		if ( "auto".equals(releaseModeName) ) {
			releaseMode = serviceRegistry.getService( TransactionFactory.class ).getDefaultReleaseMode();
		}
		else {
			releaseMode = ConnectionReleaseMode.parse( releaseModeName );
		}		
		settings.setConnectionReleaseMode( releaseMode );
		
		//sql设置
		String defaultSchema = properties.getProperty( AvailableSettings.DEFAULT_SCHEMA );
		String defaultCatalog = properties.getProperty( AvailableSettings.DEFAULT_CATALOG );
		settings.setDefaultSchemaName( defaultSchema );
		settings.setDefaultCatalogName( defaultCatalog );
		
		Integer maxFetchDepth = ConfigurationHelper.getInteger( AvailableSettings.MAX_FETCH_DEPTH, properties );
		settings.setMaximumFetchDepth( maxFetchDepth );
		
		int batchFetchSize = ConfigurationHelper.getInt(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, properties, 1);
		settings.setDefaultBatchFetchSize( batchFetchSize );
		
		boolean orderUpdates = ConfigurationHelper.getBoolean( AvailableSettings.ORDER_UPDATES, properties );
		settings.setOrderUpdatesEnabled( orderUpdates );
		
		boolean orderInserts = ConfigurationHelper.getBoolean(AvailableSettings.ORDER_INSERTS, properties);
		settings.setOrderInsertsEnabled( orderInserts );
		
		//Query解析设置
		settings.setQueryTranslatorFactory( createQueryTranslatorFactory( properties, serviceRegistry ) );
		
		Map querySubstitutions = ConfigurationHelper.toMap( AvailableSettings.QUERY_SUBSTITUTIONS, " ,=;:\n\t\r\f", properties );
		settings.setQuerySubstitutions( querySubstitutions );
		
		boolean jpaqlCompliance = ConfigurationHelper.getBoolean( AvailableSettings.JPAQL_STRICT_COMPLIANCE, properties, false );
		settings.setStrictJPAQLCompliance( jpaqlCompliance );
		
		boolean useSecondLevelCache = ConfigurationHelper.getBoolean( AvailableSettings.USE_SECOND_LEVEL_CACHE, properties, true );
		settings.setSecondLevelCacheEnabled( useSecondLevelCache );
		
		boolean useQueryCache = ConfigurationHelper.getBoolean(AvailableSettings.USE_QUERY_CACHE, properties);
		settings.setQueryCacheEnabled( useQueryCache );
		
		String prefix = properties.getProperty( AvailableSettings.CACHE_REGION_PREFIX );
		if ( StringHelper.isEmpty(prefix) ) {
			prefix=null;
		}
		settings.setCacheRegionPrefix( prefix );
		
		boolean useStructuredCacheEntries = ConfigurationHelper.getBoolean( AvailableSettings.USE_STRUCTURED_CACHE, properties, false );
		settings.setStructuredCacheEntriesEnabled( useStructuredCacheEntries );
		
		boolean useStatistics = ConfigurationHelper.getBoolean( AvailableSettings.GENERATE_STATISTICS, properties );
		settings.setStatisticsEnabled( useStatistics );
		
		boolean useIdentifierRollback = ConfigurationHelper.getBoolean( AvailableSettings.USE_IDENTIFIER_ROLLBACK, properties );
		settings.setIdentifierRollbackEnabled( useIdentifierRollback );
		
		EntityMode defaultEntityMode = EntityMode.parse( properties.getProperty( AvailableSettings.DEFAULT_ENTITY_MODE ) );
		settings.setDefaultEntityMode( defaultEntityMode );
		
		boolean namedQueryChecking = ConfigurationHelper.getBoolean( AvailableSettings.QUERY_STARTUP_CHECKING, properties, true );
		settings.setNamedQueryStartupCheckingEnabled( namedQueryChecking );
		
		boolean checkNullability = ConfigurationHelper.getBoolean(AvailableSettings.CHECK_NULLABILITY, properties, true);
		settings.setCheckNullability(checkNullability);
		
		boolean initializeLazyStateOutsideTransactionsEnabled = ConfigurationHelper.getBoolean(
				AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS,
				properties,
				false
		);
		settings.setInitializeLazyStateOutsideTransactions( initializeLazyStateOutsideTransactionsEnabled );
		
		return settings;
	}

	private QueryTranslatorFactory createQueryTranslatorFactory(
			Properties properties, ServiceRegistry serviceRegistry) {
		String className = ConfigurationHelper.getString(
				AvailableSettings.QUERY_TRANSLATOR, properties, "com.zorm.query.ASTQueryTranslatorFactory"
		);
		log.debug("Query translator:"+className);
		try {
			return (QueryTranslatorFactory) serviceRegistry.getService( ClassLoaderService.class )
					.classForName( className )
					.newInstance();
		}
		catch ( Exception e ) {
			throw new ZormException( "could not instantiate QueryTranslatorFactory: " + className, e );
		}
	}

}
