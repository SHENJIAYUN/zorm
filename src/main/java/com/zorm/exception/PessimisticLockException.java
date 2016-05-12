package com.zorm.exception;

import java.sql.SQLException;

public class PessimisticLockException extends JDBCException {
	public PessimisticLockException(String s, SQLException se, String sql) {
		super( s, se, sql );
	}
}