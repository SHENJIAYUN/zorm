package com.zorm.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provides callback access into the context in which the LOB is to be created.
 */
public interface LobCreationContext {
	/**
	 * The callback contract for making use of the JDBC {@link Connection}.
	 */
	public static interface Callback<T> {
		/**
		 * Perform whatever actions are necessary using the provided JDBC {@link Connection}.
		 *
		 * @param connection The JDBC {@link Connection}.
		 * @return The created LOB.
		 * @throws SQLException
		 */
		public T executeOnConnection(Connection connection) throws SQLException;
	}
	
	/**
	 * Execute the given callback, making sure it has access to a viable JDBC {@link Connection}.
	 *
	 * @param callback The callback to execute .
	 * @return The LOB created by the callback.
	 */
	public <T> T execute(Callback<T> callback);
}
