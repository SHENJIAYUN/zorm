package com.zorm.engine;

import java.sql.PreparedStatement;
import java.util.*;

import com.zorm.jdbc.Batch;
import com.zorm.jdbc.JdbcCoordinator;
import com.zorm.jdbc.SqlExceptionHelper;

public abstract class AbstractBatchImpl implements Batch{
	private final BatchKey key;
	private final JdbcCoordinator jdbcCoordinator;
	private LinkedHashMap<String,PreparedStatement> statements = new LinkedHashMap<String,PreparedStatement>();
	private LinkedHashSet<BatchObserver> observers = new LinkedHashSet<BatchObserver>();

    protected AbstractBatchImpl(BatchKey key, JdbcCoordinator jdbcCoordinator) {
		if ( key == null ) {
			throw new IllegalArgumentException( "batch key cannot be null" );
		}
		if ( jdbcCoordinator == null ) {
			throw new IllegalArgumentException( "JDBC coordinator cannot be null" );
		}
		this.key = key;
		this.jdbcCoordinator = jdbcCoordinator;
	}
    
    protected abstract void doExecuteBatch();
    
    private PreparedStatement buildBatchStatement(String sql, boolean callable) {
		return jdbcCoordinator.getStatementPreparer().prepareStatement( sql, callable );
	}
    

	@Override
	public final BatchKey getKey() {
		return key;
	}
    
    protected LinkedHashMap<String,PreparedStatement> getStatements() {
		return statements;
	}
    
    @Override
	public PreparedStatement getBatchStatement(String sql, boolean callable) {
		if ( sql == null ) {
			throw new IllegalArgumentException( "sql must be non-null." );
		}
		PreparedStatement statement = statements.get( sql );
		if ( statement == null ) {
			statement = buildBatchStatement( sql, callable );
			statements.put( sql, statement );
		}
		else {
		}
		return statement;
	}
    
    protected SqlExceptionHelper sqlExceptionHelper() {
		return jdbcCoordinator.getTransactionCoordinator()
				.getTransactionContext()
				.getTransactionEnvironment()
				.getJdbcServices()
				.getSqlExceptionHelper();
	}
    
    @Override
    public void execute() {
    	if ( statements.isEmpty() ) {
			return;
		}
		try {
			try {
				doExecuteBatch();
			}
			finally {
				releaseStatements();
			}
		}
		finally {
			statements.clear();
		}
    }
    
    private void releaseStatements() {
		
	}

	@Override
    public void release() {
		releaseStatements();
		observers.clear();
    }
}
