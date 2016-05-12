package com.zorm.exception;

import java.io.Serializable;
import java.sql.SQLException;

public interface SQLExceptionConverter extends Serializable{
	public JDBCException convert(SQLException sqlException, String message, String sql);
}
