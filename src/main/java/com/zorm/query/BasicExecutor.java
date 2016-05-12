package com.zorm.query;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import antlr.RecognitionException;

import com.zorm.action.BulkOperationCleanupAction;
import com.zorm.engine.RowSelection;
import com.zorm.event.EventSource;
import com.zorm.exception.QuerySyntaxException;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.Queryable;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;

public class BasicExecutor implements StatementExecutor {
	private final SessionFactoryImplementor factory;
	private final Queryable persister;
	private final String sql;
	private final List parameterSpecifications;

	public BasicExecutor(SqlWalker walker, Queryable persister) {
		this.factory = walker.getSessionFactoryHelper().getFactory();
		this.persister = persister;
		try {
			SqlGenerator gen = new SqlGenerator( factory );
			gen.statement( walker.getAST() );
			sql = gen.getSQL();
			gen.getParseErrorHandler().throwQueryException();
			parameterSpecifications = gen.getCollectedParameters();
		}
		catch ( RecognitionException e ) {
			throw QuerySyntaxException.convert( e );
		}
	}

	public String[] getSqlStatements() {
		return new String[] { sql };
	}

	public int execute(QueryParameters parameters, SessionImplementor session) throws ZormException {
		BulkOperationCleanupAction action = new BulkOperationCleanupAction( session, persister );
		if ( session.isEventSource() ) {
			( (EventSource) session ).getActionQueue().addAction( action );
		}
		else {
			action.getAfterTransactionCompletionProcess().doAfterTransactionCompletion( true, session );
		}

		PreparedStatement st = null;
		RowSelection selection = parameters.getRowSelection();

		try {
			try {
				st = session.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( sql, false );
				Iterator parameterSpecifications = this.parameterSpecifications.iterator();
				int pos = 1;
				while ( parameterSpecifications.hasNext() ) {
					final ParameterSpecification paramSpec = ( ParameterSpecification ) parameterSpecifications.next();
					pos += paramSpec.bind( st, parameters, session, pos );
				}
				if ( selection != null ) {
					if ( selection.getTimeout() != null ) {
						st.setQueryTimeout( selection.getTimeout().intValue() );
					}
				}

				return st.executeUpdate();
			}
			finally {
				if ( st != null ) {
					st.close();
				}
			}
		}
		catch( SQLException sqle ) {
			throw factory.getSQLExceptionHelper().convert( sqle, "could not execute update query", sql );
		}
	}
}
