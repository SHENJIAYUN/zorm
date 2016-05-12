package com.zorm.exception;

import java.sql.SQLException;
import java.util.ArrayList;

public class StandardSQLExceptionConverter implements SQLExceptionConverter {
	private ArrayList<SQLExceptionConversionDelegate> delegates = new ArrayList<SQLExceptionConversionDelegate>();

	public void addDelegate(SQLExceptionConversionDelegate delegate) {
		if ( delegate != null ) {
			this.delegates.add( delegate );
		}
	}

	@Override
	public JDBCException convert(SQLException sqlException, String message, String sql) {
		for ( SQLExceptionConversionDelegate delegate : delegates ) {
			final JDBCException jdbcException = delegate.convert( sqlException, message, sql );
			if ( jdbcException != null ) {
				return jdbcException;
			}
		}
		return new GenericJDBCException( message, sqlException, sql );
	}
}