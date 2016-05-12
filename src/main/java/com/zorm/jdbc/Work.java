package com.zorm.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Contract for performing a discrete piece of JDBC work.
 *
 * @author JIA
 */
public interface Work {
	/**
	 * Execute the discrete work encapsulated by this work instance using the supplied connection.
	 *
	 * @param connection The connection on which to perform the work.
	 * @throws SQLException Thrown during execution of the underlying JDBC interaction.
	 * @throws HibernateException Generally indicates a wrapped SQLException.
	 */
	public void execute(Connection connection) throws SQLException;
}
