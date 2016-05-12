package com.zorm.session;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.zorm.MultiTenancyStrategy;
import com.zorm.entity.EntityKey;
import com.zorm.exception.MappingException;
import com.zorm.exception.SessionException;
import com.zorm.exception.ZormException;
import com.zorm.jdbc.ConnectionProvider;
import com.zorm.jdbc.JdbcConnectionAccess;
import com.zorm.jdbc.LobCreationContext;
import com.zorm.jdbc.MultiTenantConnectionProvider;
import com.zorm.jdbc.WorkExecutor;
import com.zorm.jdbc.WorkExecutorVisitable;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.query.NativeSQLQuerySpecification;
import com.zorm.query.Query;
import com.zorm.query.QueryImpl;
import com.zorm.query.QueryParameters;
import com.zorm.query.QueryPlan;
import com.zorm.query.SQLQuery;
import com.zorm.query.ScrollableResults;
import com.zorm.transaction.TransactionContext;
import com.zorm.transaction.TransactionEnvironment;

public abstract class AbstractSessionImpl implements Serializable, SharedSessionContract,
SessionImplementor, TransactionContext{

	protected transient SessionFactoryImpl factory;
	private transient JdbcConnectionAccess jdbcConnectionAccess;
	private final String tenantIdentifier;
	private boolean closed = false;
	
	public AbstractSessionImpl(SessionFactoryImpl factory,
			String tenantIdentifier) {
		this.factory = factory;
		this.tenantIdentifier = tenantIdentifier;
		if(MultiTenancyStrategy.NONE == factory.getSettings().getMultiTenancyStrategy()){
			if(tenantIdentifier!=null){
				throw new ZormException( "SessionFactory was not configured for multi-tenancy" );
			}
		}
		else {
			if ( tenantIdentifier == null ) {
				throw new ZormException( "SessionFactory configured for multi-tenancy, but no tenant identifier specified" );
			}
		}
	}
	
	@Override
	public TransactionEnvironment getTransactionEnvironment() {
		return factory.getTransactionEnvironment();
	}
	
	protected void errorIfClosed(){
		if(closed){
			throw new SessionException("Session is closed!");
		}
	}
		
	@Override
	public JdbcConnectionAccess getJdbcConnectionAccess() {
		if ( jdbcConnectionAccess == null ) {
			if ( MultiTenancyStrategy.NONE == factory.getSettings().getMultiTenancyStrategy() ) {
				jdbcConnectionAccess = new NonContextualJdbcConnectionAccess(
						factory.getServiceRegistry().getService( ConnectionProvider.class )
				);
			}
			else {
				jdbcConnectionAccess = new ContextualJdbcConnectionAccess(
						factory.getServiceRegistry().getService( MultiTenantConnectionProvider.class )
				);
			}
		}
		return jdbcConnectionAccess;
	}
	
	@Override
	public SessionFactoryImplementor getFactory() {
		return factory;
	}
	
	@Override
	public <T> T execute(final LobCreationContext.Callback<T> callback) {
		return null;
	}
	
	@Override
	public boolean isClosed() {
		return closed;
	}
	
	protected void setClosed() {
		closed = true;
	}
	
	@Override
	public Query getNamedQuery(String queryName) throws MappingException{
		return null;
	}
    
	@Override
	public Query getNamedSQLQuery(String name) throws MappingException{
		return null;
	}
	
	@Override
	public Query createQuery(String queryString) {
		errorIfClosed();
		QueryImpl query = new QueryImpl(
				queryString,
		        this,
		        getQueryPlan( queryString, false ).getParameterMetadata()
		);
		query.setComment( queryString );
		return query;
	}
	
	protected QueryPlan getQueryPlan(String query, boolean shallow){
		return factory.getQueryPlanCache().getQueryPlan( query, shallow, getEnabledFilters() );
	}
	
	@Override
	public SQLQuery createSQLQuery(String queryString) {
		return null;
	}
	
	public List list(NativeSQLQuerySpecification spec, QueryParameters queryParameters) throws ZormException {
		return null;
	};
	
	public ScrollableResults scroll(NativeSQLQuerySpecification spec, QueryParameters queryParameters) throws ZormException {
		return null;
	};
	
	@Override
	public String getTenantIdentifier() {
		return tenantIdentifier;
	}
	
	@Override
	public EntityKey generateEntityKey(Serializable id,
			EntityPersister persister) {
		return new EntityKey(id, persister, getTenantIdentifier());
	}
	
	
	
	private static class NonContextualJdbcConnectionAccess implements JdbcConnectionAccess, Serializable {
		private final ConnectionProvider connectionProvider;

		private NonContextualJdbcConnectionAccess(ConnectionProvider connectionProvider) {
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
	
	private class ContextualJdbcConnectionAccess implements JdbcConnectionAccess, Serializable {
		private final MultiTenantConnectionProvider connectionProvider;

		private ContextualJdbcConnectionAccess(MultiTenantConnectionProvider connectionProvider) {
			this.connectionProvider = connectionProvider;
		}

		@Override
		public Connection obtainConnection() throws SQLException {
			if ( tenantIdentifier == null ) {
				throw new ZormException( "Tenant identifier required!" );
			}
			return connectionProvider.getConnection( tenantIdentifier );
		}

		@Override
		public void releaseConnection(Connection connection) throws SQLException {
			if ( tenantIdentifier == null ) {
				throw new ZormException( "Tenant identifier required!" );
			}
			connectionProvider.releaseConnection( tenantIdentifier, connection );
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return connectionProvider.supportsAggressiveRelease();
		}
	}

}
