package com.zorm.config;

import java.util.Map;

import com.zorm.ConnectionReleaseMode;
import com.zorm.MultiTenancyStrategy;
import com.zorm.entity.EntityMode;
import com.zorm.entity.EntityTuplizerFactory;
import com.zorm.query.QueryTranslatorFactory;
import com.zorm.service.jta.platform.spi.JtaPlatform;

/**
 * Settings that affect the behaviour of Hibernate at runtime.
 *
 * @author Gavin King
 */
public final class Settings {

	private Integer maximumFetchDepth;
	private Map querySubstitutions;
	private int jdbcBatchSize;
	private int defaultBatchFetchSize;
	private boolean scrollableResultSetsEnabled;
	private boolean getGeneratedKeysEnabled;
	private String defaultSchemaName;
	private String defaultCatalogName;
	private Integer jdbcFetchSize;
	private String sessionFactoryName;
	private boolean sessionFactoryNameAlsoJndiName;
	private boolean autoCreateSchema;
	private boolean autoDropSchema;
	private boolean autoUpdateSchema;
	private boolean autoValidateSchema;
	private boolean queryCacheEnabled;
	private boolean structuredCacheEntriesEnabled;
	private boolean secondLevelCacheEnabled;
	private String cacheRegionPrefix;
	private boolean minimalPutsEnabled;
	private boolean commentsEnabled;
	private boolean statisticsEnabled;
	private boolean jdbcBatchVersionedData;
	private boolean identifierRollbackEnabled;
	private boolean flushBeforeCompletionEnabled;
	private boolean autoCloseSessionEnabled;
	private boolean wrapResultSetsEnabled;
	private boolean orderUpdatesEnabled;
	private boolean orderInsertsEnabled;
	private boolean dataDefinitionImplicitCommit;
	private boolean dataDefinitionInTransactionSupported;
	private boolean strictJPAQLCompliance;
	private boolean namedQueryStartupCheckingEnabled;
	private boolean checkNullability;
	private boolean initializeLazyStateOutsideTransactions;
//	private ComponentTuplizerFactory componentTuplizerFactory; todo : HHH-3517 and HHH-1907
//	private BytecodeProvider bytecodeProvider;
	private String importFiles;
	private JtaPlatform jtaPlatform;
	private EntityMode defaultEntityMode;
	private ConnectionReleaseMode connectionReleaseMode;
	private RegionFactory regionFactory;
	private MultiTenancyStrategy multiTenancyStrategy;
	private EntityTuplizerFactory entityTuplizerFactory;
	private QueryTranslatorFactory queryTranslatorFactory;

	/**
	 * Package protected constructor
	 */
	Settings() {
	}

	void setRegionFactory(RegionFactory regionFactory) {
		this.regionFactory = regionFactory;
	}

	void setMultiTenancyStrategy(MultiTenancyStrategy multiTenancyStrategy) {
		this.multiTenancyStrategy = multiTenancyStrategy;
	}
	
	public String getImportFiles() {
		return importFiles;
	}

	public void setImportFiles(String importFiles) {
		this.importFiles = importFiles;
	}

	public String getDefaultSchemaName() {
		return defaultSchemaName;
	}

	public String getDefaultCatalogName() {
		return defaultCatalogName;
	}

	public int getJdbcBatchSize() {
		return jdbcBatchSize;
	}

	public int getDefaultBatchFetchSize() {
		return defaultBatchFetchSize;
	}

	public Map getQuerySubstitutions() {
		return querySubstitutions;
	}

	public boolean isIdentifierRollbackEnabled() {
		return identifierRollbackEnabled;
	}

	public boolean isScrollableResultSetsEnabled() {
		return scrollableResultSetsEnabled;
	}

	public boolean isGetGeneratedKeysEnabled() {
		return getGeneratedKeysEnabled;
	}

	public boolean isMinimalPutsEnabled() {
		return minimalPutsEnabled;
	}

	public Integer getJdbcFetchSize() {
		return jdbcFetchSize;
	}

	public String getSessionFactoryName() {
		return sessionFactoryName;
	}

	public boolean isSessionFactoryNameAlsoJndiName() {
		return sessionFactoryNameAlsoJndiName;
	}

	public boolean isAutoCreateSchema() {
		return autoCreateSchema;
	}

	public boolean isAutoDropSchema() {
		return autoDropSchema;
	}

	public boolean isAutoUpdateSchema() {
		return autoUpdateSchema;
	}

	public Integer getMaximumFetchDepth() {
		return maximumFetchDepth;
	}

	public boolean isQueryCacheEnabled() {
		return queryCacheEnabled;
	}

	public boolean isCommentsEnabled() {
		return commentsEnabled;
	}

	public boolean isSecondLevelCacheEnabled() {
		return secondLevelCacheEnabled;
	}

	public String getCacheRegionPrefix() {
		return cacheRegionPrefix;
	}

	public boolean isStatisticsEnabled() {
		return statisticsEnabled;
	}

	public boolean isJdbcBatchVersionedData() {
		return jdbcBatchVersionedData;
	}

	public boolean isFlushBeforeCompletionEnabled() {
		return flushBeforeCompletionEnabled;
	}

	public boolean isAutoCloseSessionEnabled() {
		return autoCloseSessionEnabled;
	}

	public boolean isWrapResultSetsEnabled() {
		return wrapResultSetsEnabled;
	}

	public boolean isOrderUpdatesEnabled() {
		return orderUpdatesEnabled;
	}

	public boolean isOrderInsertsEnabled() {
		return orderInsertsEnabled;
	}

	public boolean isStructuredCacheEntriesEnabled() {
		return structuredCacheEntriesEnabled;
	}

	public boolean isAutoValidateSchema() {
		return autoValidateSchema;
	}

	public boolean isDataDefinitionImplicitCommit() {
		return dataDefinitionImplicitCommit;
	}

	public boolean isDataDefinitionInTransactionSupported() {
		return dataDefinitionInTransactionSupported;
	}

	public boolean isStrictJPAQLCompliance() {
		return strictJPAQLCompliance;
	}

	public boolean isNamedQueryStartupCheckingEnabled() {
		return namedQueryStartupCheckingEnabled;
	}


	// package protected setters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	void setDefaultSchemaName(String string) {
		defaultSchemaName = string;
	}

	void setDefaultCatalogName(String string) {
		defaultCatalogName = string;
	}

	void setJdbcBatchSize(int i) {
		jdbcBatchSize = i;
	}

	void setDefaultBatchFetchSize(int i) {
		defaultBatchFetchSize = i;
	}

	void setQuerySubstitutions(Map map) {
		querySubstitutions = map;
	}

	void setIdentifierRollbackEnabled(boolean b) {
		identifierRollbackEnabled = b;
	}

	void setMinimalPutsEnabled(boolean b) {
		minimalPutsEnabled = b;
	}

	void setScrollableResultSetsEnabled(boolean b) {
		scrollableResultSetsEnabled = b;
	}

	void setGetGeneratedKeysEnabled(boolean b) {
		getGeneratedKeysEnabled = b;
	}

	void setJdbcFetchSize(Integer integer) {
		jdbcFetchSize = integer;
	}

	void setSessionFactoryName(String string) {
		sessionFactoryName = string;
	}

	void setSessionFactoryNameAlsoJndiName(boolean sessionFactoryNameAlsoJndiName) {
		this.sessionFactoryNameAlsoJndiName = sessionFactoryNameAlsoJndiName;
	}

	void setAutoCreateSchema(boolean b) {
		autoCreateSchema = b;
	}

	void setAutoDropSchema(boolean b) {
		autoDropSchema = b;
	}

	void setAutoUpdateSchema(boolean b) {
		autoUpdateSchema = b;
	}

	void setMaximumFetchDepth(Integer i) {
		maximumFetchDepth = i;
	}

	void setQueryCacheEnabled(boolean b) {
		queryCacheEnabled = b;
	}

	void setCommentsEnabled(boolean commentsEnabled) {
		this.commentsEnabled = commentsEnabled;
	}

	void setSecondLevelCacheEnabled(boolean secondLevelCacheEnabled) {
		this.secondLevelCacheEnabled = secondLevelCacheEnabled;
	}

	void setCacheRegionPrefix(String cacheRegionPrefix) {
		this.cacheRegionPrefix = cacheRegionPrefix;
	}

	void setStatisticsEnabled(boolean statisticsEnabled) {
		this.statisticsEnabled = statisticsEnabled;
	}

	void setJdbcBatchVersionedData(boolean jdbcBatchVersionedData) {
		this.jdbcBatchVersionedData = jdbcBatchVersionedData;
	}

	void setFlushBeforeCompletionEnabled(boolean flushBeforeCompletionEnabled) {
		this.flushBeforeCompletionEnabled = flushBeforeCompletionEnabled;
	}

	void setAutoCloseSessionEnabled(boolean autoCloseSessionEnabled) {
		this.autoCloseSessionEnabled = autoCloseSessionEnabled;
	}

	void setWrapResultSetsEnabled(boolean wrapResultSetsEnabled) {
		this.wrapResultSetsEnabled = wrapResultSetsEnabled;
	}

	void setOrderUpdatesEnabled(boolean orderUpdatesEnabled) {
		this.orderUpdatesEnabled = orderUpdatesEnabled;
	}

	void setOrderInsertsEnabled(boolean orderInsertsEnabled) {
		this.orderInsertsEnabled = orderInsertsEnabled;
	}

	void setStructuredCacheEntriesEnabled(boolean structuredCacheEntriesEnabled) {
		this.structuredCacheEntriesEnabled = structuredCacheEntriesEnabled;
	}

	void setAutoValidateSchema(boolean autoValidateSchema) {
		this.autoValidateSchema = autoValidateSchema;
	}

	void setDataDefinitionImplicitCommit(boolean dataDefinitionImplicitCommit) {
		this.dataDefinitionImplicitCommit = dataDefinitionImplicitCommit;
	}

	void setDataDefinitionInTransactionSupported(boolean dataDefinitionInTransactionSupported) {
		this.dataDefinitionInTransactionSupported = dataDefinitionInTransactionSupported;
	}

	void setStrictJPAQLCompliance(boolean strictJPAQLCompliance) {
		this.strictJPAQLCompliance = strictJPAQLCompliance;
	}

	void setNamedQueryStartupCheckingEnabled(boolean namedQueryStartupCheckingEnabled) {
		this.namedQueryStartupCheckingEnabled = namedQueryStartupCheckingEnabled;
	}

	public boolean isCheckNullability() {
		return checkNullability;
	}

	public void setCheckNullability(boolean checkNullability) {
		this.checkNullability = checkNullability;
	}

	//	void setComponentTuplizerFactory(ComponentTuplizerFactory componentTuplizerFactory) {
//		this.componentTuplizerFactory = componentTuplizerFactory;
//	}

	//	public BytecodeProvider getBytecodeProvider() {
//		return bytecodeProvider;
//	}
//
//	void setBytecodeProvider(BytecodeProvider bytecodeProvider) {
//		this.bytecodeProvider = bytecodeProvider;
//	}


	public boolean isInitializeLazyStateOutsideTransactionsEnabled() {
		return initializeLazyStateOutsideTransactions;
	}

	void setInitializeLazyStateOutsideTransactions(boolean initializeLazyStateOutsideTransactions) {
		this.initializeLazyStateOutsideTransactions = initializeLazyStateOutsideTransactions;
	}

	public void setJtaPlatform(JtaPlatform jtaPlatform) {
		this.jtaPlatform = jtaPlatform;
	}

	void setDefaultEntityMode(EntityMode defaultEntityMode) {
		this.defaultEntityMode = defaultEntityMode;
	}

	public ConnectionReleaseMode getConnectionReleaseMode() {
		return connectionReleaseMode;
	}

	public RegionFactory getRegionFactory() {
		return regionFactory;
	}

	public MultiTenancyStrategy getMultiTenancyStrategy() {
		return multiTenancyStrategy;
	}

	public EntityTuplizerFactory getEntityTuplizerFactory() {
		return entityTuplizerFactory;
	}

	public void setConnectionReleaseMode(ConnectionReleaseMode releaseMode) {
		this.connectionReleaseMode = releaseMode;
	}
	
	void setEntityTuplizerFactory(EntityTuplizerFactory entityTuplizerFactory) {
		this.entityTuplizerFactory = entityTuplizerFactory;
	}

	public QueryTranslatorFactory getQueryTranslatorFactory() {
		return queryTranslatorFactory;
	}

	void setQueryTranslatorFactory(QueryTranslatorFactory queryTranslatorFactory) {
		this.queryTranslatorFactory = queryTranslatorFactory;
	}
}
