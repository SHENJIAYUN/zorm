package com.zorm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zorm.engine.InvalidatableWrapper;
import com.zorm.engine.JdbcWrapper;

public class JdbcResourceRegistryImpl implements JdbcResourceRegistry {
	
	private final HashMap<Statement,Set<ResultSet>> xref = new HashMap<Statement,Set<ResultSet>>();
	private final Set<ResultSet> unassociatedResultSets = new HashSet<ResultSet>();
	private final SqlExceptionHelper exceptionHelper;
	private Statement lastQuery;
	
	public JdbcResourceRegistryImpl(SqlExceptionHelper exceptionHelper) {
		this.exceptionHelper = exceptionHelper;
	}

	@Override
	public void register(Statement statement) {
		xref.put( statement, null );
	}

	@Override
	public boolean hasRegisteredResources() {
		return ! xref.isEmpty() || ! unassociatedResultSets.isEmpty();
	}

	@Override
	public void releaseResources() {
		cleanup();
	}
	
	private void cleanup() {
		for ( Map.Entry<Statement,Set<ResultSet>> entry : xref.entrySet() ) {
			if ( entry.getValue() != null ) {
				closeAll( entry.getValue() );
			}
			close( entry.getKey() );
		}
		xref.clear();

		closeAll( unassociatedResultSets );
	}

	protected void closeAll(Set<ResultSet> resultSets) {
		for ( ResultSet resultSet : resultSets ) {
			close( resultSet );
		}
		resultSets.clear();
	}
	
	protected void close(Statement statement) {

		if ( statement instanceof InvalidatableWrapper ) {
			InvalidatableWrapper<Statement> wrapper = ( InvalidatableWrapper<Statement> ) statement;
			close( wrapper.getWrappedObject() );
			wrapper.invalidate();
			return;
		}

		try {
			// if we are unable to "clean" the prepared statement,
			// we do not close it
			try {
				if ( statement.getMaxRows() != 0 ) {
					statement.setMaxRows( 0 );
				}
				if ( statement.getQueryTimeout() != 0 ) {
					statement.setQueryTimeout( 0 );
				}
			}
			catch( SQLException sqle ) {
				return; // EARLY EXIT!!!
			}
			statement.close();
			if ( lastQuery == statement ) {
				lastQuery = null;
			}
		}
		catch( SQLException e ) {
		}
		catch ( Exception e ) {
			// try to handle general errors more elegantly
		}
	}
	
	protected void close(ResultSet resultSet) {

		if ( resultSet instanceof InvalidatableWrapper ) {
			InvalidatableWrapper<ResultSet> wrapper = (InvalidatableWrapper<ResultSet>) resultSet;
			close( wrapper.getWrappedObject() );
			wrapper.invalidate();
			return;
		}

		try {
			resultSet.close();
		}
		catch( SQLException e ) {
		}
		catch ( Exception e ) {
			// try to handle general errors more elegantly
		}
	}

	@Override
	public void register(ResultSet resultSet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void release(ResultSet proxy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void registerLastQuery(Statement statement) {
		if ( statement instanceof JdbcWrapper ) {
			JdbcWrapper<Statement> wrapper = ( JdbcWrapper<Statement> ) statement;
			registerLastQuery( wrapper.getWrappedObject() );
			return;
		}
		lastQuery = statement;
	}
	
	@Override
	public void close() {
		cleanup();
	}
}
