package com.zorm.jdbc;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

public interface JdbcConnectionAccess extends Serializable{
	/**
	 * Obtain a JDBC connection
	 *
	 * @return The obtained connection
	 *
	 * @throws SQLException Indicates a problem getting the connection
	 */
	public Connection obtainConnection() throws SQLException;
	
	/**
	 * Release a previously obtained connection
	 *
	 * @param connection The connection to release
	 *
	 * @throws SQLException Indicates a problem releasing the connection
	 */
	public void releaseConnection(Connection connection) throws SQLException;
	
	/**
	 * Does the underlying provider of connections support aggressive releasing of connections (and re-acquisition
	 * of those connections later, if need be) in JTA environments?
	 *
	 * @see org.hibernate.service.jdbc.connections.spi.ConnectionProvider#supportsAggressiveRelease()
	 * @see org.hibernate.service.jdbc.connections.spi.MultiTenantConnectionProvider#supportsAggressiveRelease()
	 */
	public boolean supportsAggressiveRelease();
}
