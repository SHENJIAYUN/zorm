package com.zorm.exception;

import java.sql.SQLException;

public interface SQLExceptionConversionDelegate {
	public JDBCException convert(SQLException sqlException, String message, String sql);
}
