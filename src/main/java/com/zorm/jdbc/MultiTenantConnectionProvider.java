package com.zorm.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import com.zorm.service.Service;
import com.zorm.service.Wrapped;

public interface MultiTenantConnectionProvider extends Service,Wrapped{
	public Connection getAnyConnection() throws SQLException;
	public void releaseAnyConnection(Connection connection) throws SQLException;
	public Connection getConnection(String tenantIdentifier) throws SQLException;
	public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException;
	public boolean supportsAggressiveRelease();
}
