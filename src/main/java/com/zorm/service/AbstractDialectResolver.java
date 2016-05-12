package com.zorm.service;

import java.sql.*;

import com.zorm.dialect.Dialect;
import com.zorm.exception.JDBCConnectionException;
import com.zorm.exception.JDBCException;

public abstract class AbstractDialectResolver implements DialectResolver {

	public final Dialect resolveDialect(DatabaseMetaData metaData) {
		try {
			return resolveDialectInternal( metaData );
		}
		catch ( SQLException sqlException ) {
            return null;
		}
		catch ( Throwable t ) {
			return null;
		}
	}

	/**
	 * Perform the actual resolution without caring about handling {@link SQLException}s.
	 *
	 * @param metaData The database metadata
	 * @return The resolved dialect, or null if we could not resolve.
	 * @throws SQLException Indicates problems accessing the metadata.
	 */
	protected abstract Dialect resolveDialectInternal(DatabaseMetaData metaData) throws SQLException;
}

