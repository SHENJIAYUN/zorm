package com.zorm.exception;

import java.sql.SQLException;

public class QueryTimeoutException extends JDBCException {

	public QueryTimeoutException( String s, JDBCException je, String sql ) {
		super(s, je.getSQLException(), sql);
	}

	public QueryTimeoutException( String s, SQLException se, String sql ) {
		super(s, se, sql);
	}

}