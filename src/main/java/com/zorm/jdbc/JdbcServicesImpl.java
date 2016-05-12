package com.zorm.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.zorm.MultiTenancyStrategy;
import com.zorm.config.Environment;
import com.zorm.dialect.Dialect;
import com.zorm.dialect.DialectFactory;
import com.zorm.exception.SQLExceptionConverter;
import com.zorm.exception.SQLExceptionTypeDelegate;
import com.zorm.exception.SQLStateConversionDelegate;
import com.zorm.exception.StandardSQLExceptionConverter;
import com.zorm.service.Configurable;
import com.zorm.service.ServiceRegistryAwareService;
import com.zorm.service.ServiceRegistryImplementor;
import com.zorm.type.descriptor.LobCreator;
import com.zorm.util.ConfigurationHelper;
import com.zorm.util.ReflectHelper;

public class JdbcServicesImpl implements JdbcServices,
        ServiceRegistryAwareService,Configurable    
{
	private ExtractedDatabaseMetaData extractedMetaDataSupport;
	private Dialect dialect;
	private ServiceRegistryImplementor serviceRegistry;
	private ConnectionProvider connectionProvider;
	private SqlExceptionHelper sqlExceptionHelper;
	
	@Override
	public void configure(Map configValues) {
		final JdbcConnectionAccess jdbcConnectionAccess = buildJdbcConnectionAccess( configValues );
		final DialectFactory dialectFactory = serviceRegistry.getService( DialectFactory.class );
	    
		Dialect dialect = null;
		//LobCreatorBuilder lobCreatorBuilder = null;

		boolean metaSupportsScrollable = false;
		boolean metaSupportsGetGeneratedKeys = false;
		boolean metaSupportsBatchUpdates = false;
		boolean metaReportsDDLCausesTxnCommit = false;
		boolean metaReportsDDLInTxnSupported = true;
		String extraKeywordsString = "";
		int sqlStateType = -1;
		boolean lobLocatorUpdateCopy = false;
		String catalogName = null;
		String schemaName = null;
		LinkedHashSet<TypeInfo> typeInfoSet = new LinkedHashSet<TypeInfo>();
		
		boolean useJdbcMetadata = ConfigurationHelper.getBoolean( "zorm.temp.use_jdbc_metadata_defaults", configValues, true );
	    if(useJdbcMetadata){
	    	try{
	    		//从连接池里获取数据库连接
	    		Connection connection = jdbcConnectionAccess.obtainConnection();
	    		try{
	    			//连接的元数据
	    		  DatabaseMetaData meta = connection.getMetaData();
	    		  
	    		  metaSupportsScrollable = meta.supportsResultSetType( ResultSet.TYPE_SCROLL_INSENSITIVE );
					metaSupportsBatchUpdates = meta.supportsBatchUpdates();
					metaReportsDDLCausesTxnCommit = meta.dataDefinitionCausesTransactionCommit();
					metaReportsDDLInTxnSupported = !meta.dataDefinitionIgnoredInTransactions();
					metaSupportsGetGeneratedKeys = meta.supportsGetGeneratedKeys();
					extraKeywordsString = meta.getSQLKeywords();
					sqlStateType = meta.getSQLStateType();
					lobLocatorUpdateCopy = meta.locatorsUpdateCopy();
					
					typeInfoSet.addAll( TypeInfoExtracter.extractTypeInfo( meta ) );
	    		
					dialect = dialectFactory.buildDialect( configValues, connection );
	    		    catalogName = connection.getCatalog();
	    		    SchemaNameResolver schemaNameResolver = determineExplicitSchemaNameResolver( configValues );
	    		
	    		    if(schemaNameResolver != null){
	    		    	schemaName = schemaNameResolver.resolveSchemaName( connection );
	    		    }
	    		    //lobCreatorBuilder = new LobCreatorBuilder( configValues, connection );
	    		}
	    		catch(SQLException e){
	    		}
	    		finally{
	    			if ( connection != null ) {
						jdbcConnectionAccess.releaseConnection( connection );
					}	
	    		}
	    	}
	    	catch ( SQLException sqle ) {
				dialect = dialectFactory.buildDialect( configValues, null );
			}
			catch ( UnsupportedOperationException uoe ) {
				// user supplied JDBC connections
				dialect = dialectFactory.buildDialect( configValues, null );
			}
	    }
	    else{
	    	dialect = dialectFactory.buildDialect( configValues, null );
	    }
		this.dialect = dialect;
		
		this.extractedMetaDataSupport = new ExtractedDatabaseMetaDataImpl(
				metaSupportsScrollable,
				metaSupportsGetGeneratedKeys,
				metaSupportsBatchUpdates,
				metaReportsDDLInTxnSupported,
				metaReportsDDLCausesTxnCommit,
				parseKeywords( extraKeywordsString ),
				parseSQLStateType( sqlStateType ),
				lobLocatorUpdateCopy,
				schemaName,
				catalogName,
				typeInfoSet
		);
		
		SQLExceptionConverter sqlExceptionConverter = dialect.buildSQLExceptionConverter();
		if ( sqlExceptionConverter == null ) {
			final StandardSQLExceptionConverter converter = new StandardSQLExceptionConverter();
			sqlExceptionConverter = converter;
			converter.addDelegate( dialect.buildSQLExceptionConversionDelegate() );
			converter.addDelegate( new SQLExceptionTypeDelegate( dialect ) );
			converter.addDelegate( new SQLStateConversionDelegate( dialect ) );
		}
		this.sqlExceptionHelper = new SqlExceptionHelper( sqlExceptionConverter );
		
	}
	
	private ExtractedDatabaseMetaData.SQLStateType parseSQLStateType(int sqlStateType) {
        switch (sqlStateType) {
		case DatabaseMetaData.sqlStateSQL99:
			return ExtractedDatabaseMetaData.SQLStateType.SQL99;
		case DatabaseMetaData.sqlStateXOpen:
			return ExtractedDatabaseMetaData.SQLStateType.XOpen;
		default:
			return ExtractedDatabaseMetaData.SQLStateType.UNKOWN;
		}
	}

	private Set<String> parseKeywords(String extraKeywordsString) {
        Set<String> keywordSet = new HashSet<String>();
        keywordSet.addAll(Arrays.asList(extraKeywordsString.split(",")));
		return keywordSet;
	}

	public static final String SCHEMA_NAME_RESOLVER = "zorm.schema_name_resolver";
	
	private SchemaNameResolver determineExplicitSchemaNameResolver(
			Map configValues) {
		Object setting = configValues.get( SCHEMA_NAME_RESOLVER );
		if ( SchemaNameResolver.class.isInstance( setting ) ) {
			return (SchemaNameResolver) setting;
		}
		String resolverClassName = (String) setting;
		if(resolverClassName!=null){
			//
		}
		return null;
	}

	private JdbcConnectionAccess buildJdbcConnectionAccess(Map configValues) {
		final MultiTenancyStrategy multiTenancyStrategy = MultiTenancyStrategy.determineMultiTenancyStrategy( configValues );
		if(MultiTenancyStrategy.NONE == multiTenancyStrategy){
			connectionProvider=serviceRegistry.getService(ConnectionProvider.class);
			return new ConnectionProviderJdbcConnectionAccess(connectionProvider);
		}
		else{
			connectionProvider = null;
			final MultiTenantConnectionProvider multiTenantConnectionProvider = serviceRegistry.getService( MultiTenantConnectionProvider.class );
			return new MultiTenantConnectionProviderJdbcConnectionAccess( multiTenantConnectionProvider );
		}
	}
	
	private static class MultiTenantConnectionProviderJdbcConnectionAccess implements JdbcConnectionAccess {
		private final MultiTenantConnectionProvider connectionProvider;

		public MultiTenantConnectionProviderJdbcConnectionAccess(MultiTenantConnectionProvider connectionProvider) {
			this.connectionProvider = connectionProvider;
		}

		@Override
		public Connection obtainConnection() throws SQLException {
			return connectionProvider.getAnyConnection();
		}

		@Override
		public void releaseConnection(Connection connection) throws SQLException {
			connectionProvider.releaseAnyConnection( connection );
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return connectionProvider.supportsAggressiveRelease();
		}
	}
	
	private static class ConnectionProviderJdbcConnectionAccess implements JdbcConnectionAccess{
		private final ConnectionProvider connectionProvider;
		public ConnectionProviderJdbcConnectionAccess(ConnectionProvider connectionProvider) {
			this.connectionProvider = connectionProvider;
		}
		@Override
		public Connection obtainConnection() throws SQLException {
			return connectionProvider.getConnection();
		}
		@Override
		public void releaseConnection(Connection connection) throws SQLException {
			connectionProvider.closeConnection( connection );
		}
		@Override
		public boolean supportsAggressiveRelease() {
			return connectionProvider.supportsAggressiveRelease();
		}
	}

	@Override
	public ExtractedDatabaseMetaData getExtractedMetaDataSupport() {
		return extractedMetaDataSupport;
	}
	
	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
	
	@Override
	public Dialect getDialect() {
		return dialect;
	}
	
	private static class ExtractedDatabaseMetaDataImpl implements ExtractedDatabaseMetaData{
		
		private final boolean supportsScrollableResults;
		private final boolean supportsGetGeneratedKeys;
		private final boolean supportsBatchUpdates;
		private final boolean supportsDataDefinitionInTransaction;
		private final boolean doesDataDefinitionCauseTransactionCommit;
		private final Set<String> extraKeywords;
		private final SQLStateType sqlStateType;
		private final boolean lobLocatorUpdateCopy;
		private final String connectionSchemaName;
		private final String connectionCatalogName;
		private final LinkedHashSet<TypeInfo> typeInfoSet;
		
		private ExtractedDatabaseMetaDataImpl(
				boolean supportsScrollableResults,
				boolean supportsGetGeneratedKeys,
				boolean supportsBatchUpdates,
				boolean supportsDataDefinitionInTransaction,
				boolean doesDataDefinitionCauseTransactionCommit,
				Set<String> extraKeywords,
				SQLStateType sqlStateType,
				boolean lobLocatorUpdateCopy,
				String connectionSchemaName,
				String connectionCatalogName,
				LinkedHashSet<TypeInfo> typeInfoSet) {
			this.supportsScrollableResults = supportsScrollableResults;
			this.supportsGetGeneratedKeys = supportsGetGeneratedKeys;
			this.supportsBatchUpdates = supportsBatchUpdates;
			this.supportsDataDefinitionInTransaction = supportsDataDefinitionInTransaction;
			this.doesDataDefinitionCauseTransactionCommit = doesDataDefinitionCauseTransactionCommit;
			this.extraKeywords = extraKeywords;
			this.sqlStateType = sqlStateType;
			this.lobLocatorUpdateCopy = lobLocatorUpdateCopy;
			this.connectionSchemaName = connectionSchemaName;
			this.connectionCatalogName = connectionCatalogName;
			this.typeInfoSet = typeInfoSet;
		}
		
		@Override
		public boolean supportsGetGeneratedKeys() {
			return supportsGetGeneratedKeys;
		}
		
		@Override
		public boolean doesDataDefinitionCauseTransactionCommit() {
			return doesDataDefinitionCauseTransactionCommit;
		}
		
		@Override
		public boolean supportsDataDefinitionInTransaction() {
			return supportsDataDefinitionInTransaction;
		}
		
		@Override
		public boolean supportsBatchUpdates() {
			return supportsBatchUpdates;
		}
		
		@Override
		public boolean supportsScrollableResults() {
			return supportsScrollableResults;
		}
	}

	@Override
	public SqlExceptionHelper getSqlExceptionHelper() {
		return sqlExceptionHelper;
	}

	@Override
	public LobCreator getLobCreator(LobCreationContext lobCreationContext) {
		return null;
	}
}
