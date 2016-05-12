package com.zorm.dialect;

import java.sql.SQLException;

import com.zorm.exception.JDBCException;
import com.zorm.exception.LockAcquisitionException;
import com.zorm.exception.LockTimeoutException;
import com.zorm.exception.SQLExceptionConversionDelegate;
import com.zorm.exception.ViolatedConstraintNameExtracter;
import com.zorm.util.JdbcExceptionHelper;

public class MySQLDialect extends Dialect {


	public boolean supportsLimit() {
		return true;
	}
	
	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new SQLExceptionConversionDelegate() {
			@Override
			public JDBCException convert(SQLException sqlException, String message, String sql) {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );

				if ( "41000".equals( sqlState ) ) {
					return new LockTimeoutException( message, sqlException, sql );
				}

				if ( "40001".equals( sqlState ) ) {
					return new LockAcquisitionException( message, sqlException, sql );
				}

				return null;
			}
		};
	}
	
	
}
