package com.zorm.service;

import java.sql.*;

import com.zorm.dialect.Dialect;
import com.zorm.dialect.MySQLDialect;
import com.zorm.exception.JDBCConnectionException;

public class StandardDialectResolver extends AbstractDialectResolver {


	@Override
    protected Dialect resolveDialectInternal(DatabaseMetaData metaData) throws SQLException {
		String databaseName = metaData.getDatabaseProductName();
		int databaseMajorVersion = metaData.getDatabaseMajorVersion();

		if ( "MySQL".equals( databaseName ) ) {
			return new MySQLDialect();
		}	
		
		return null;
	}
}
