package com.zorm.service;

import java.sql.DatabaseMetaData;

import com.zorm.dialect.Dialect;
import com.zorm.exception.JDBCConnectionException;

public interface DialectResolver extends Service{
	public Dialect resolveDialect(DatabaseMetaData metaData) throws JDBCConnectionException;
}
