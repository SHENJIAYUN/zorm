package com.zorm.exception;

import java.sql.DataTruncation;
import java.sql.SQLClientInfoException;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransactionRollbackException;
import java.sql.SQLTransientConnectionException;

public class SQLExceptionTypeDelegate extends AbstractSQLExceptionConversionDelegate {
	public SQLExceptionTypeDelegate(ConversionContext conversionContext) {
		super( conversionContext );
	}


	@Override
	public JDBCException convert(SQLException sqlException, String message, String sql) {
		if ( SQLClientInfoException.class.isInstance( sqlException )
				|| SQLInvalidAuthorizationSpecException.class.isInstance( sqlException )
				|| SQLNonTransientConnectionException.class.isInstance( sqlException )
				|| SQLTransientConnectionException.class.isInstance( sqlException ) ) {
			return new JDBCConnectionException( message, sqlException, sql );
		}
		else if ( DataTruncation.class.isInstance( sqlException ) ||
				SQLDataException.class.isInstance( sqlException ) ) {
			throw new DataException( message, sqlException, sql );
		}
		else if ( SQLIntegrityConstraintViolationException.class.isInstance( sqlException ) ) {
			return new ConstraintViolationException(
					message,
					sqlException,
					sql,
					getConversionContext().getViolatedConstraintNameExtracter().extractConstraintName( sqlException )
			);
		}
		else if ( SQLSyntaxErrorException.class.isInstance( sqlException ) ) {
			return new SQLGrammarException( message, sqlException, sql );
		}
		else if ( SQLTimeoutException.class.isInstance( sqlException ) ) {
			return new QueryTimeoutException( message, sqlException, sql );
		}
		else if ( SQLTransactionRollbackException.class.isInstance( sqlException ) ) {
			return new LockAcquisitionException( message, sqlException, sql );
		}

		return null; 
	}
}
