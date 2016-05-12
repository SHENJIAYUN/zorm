package com.zorm.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import com.zorm.exception.JDBCException;
import com.zorm.exception.SQLExceptionConverter;
import com.zorm.exception.SQLStateConverter;
import com.zorm.exception.ViolatedConstraintNameExtracter;

public class SqlExceptionHelper {


	public static final String DEFAULT_EXCEPTION_MSG = "SQL Exception";
	public static final String DEFAULT_WARNING_MSG = "SQL Warning";

	public static final SQLExceptionConverter DEFAULT_CONVERTER = new SQLStateConverter(
			new ViolatedConstraintNameExtracter() {
				public String extractConstraintName(SQLException e) {
					return null;
				}
			}
	);

	private SQLExceptionConverter sqlExceptionConverter;

	/**
	 * Create an exception helper with a default exception converter.
	 */
	public SqlExceptionHelper() {
		sqlExceptionConverter = DEFAULT_CONVERTER;
	}

	/**
	 * Create an exception helper with a specific exception converter.
	 *
	 * @param sqlExceptionConverter The exception converter to use.
	 */
	public SqlExceptionHelper(SQLExceptionConverter sqlExceptionConverter) {
		this.sqlExceptionConverter = sqlExceptionConverter;
	}

	/**
	 * Access the current exception converter being used internally.
	 *
	 * @return The current exception converter.
	 */
	public SQLExceptionConverter getSqlExceptionConverter() {
		return sqlExceptionConverter;
	}

	/**
	 * Inject the exception converter to use.
	 * <p/>
	 * NOTE : <tt>null</tt> is allowed and signifies to use the default.
	 *
	 * @param sqlExceptionConverter The converter to use.
	 */
	public void setSqlExceptionConverter(SQLExceptionConverter sqlExceptionConverter) {
		this.sqlExceptionConverter = ( sqlExceptionConverter == null ? DEFAULT_CONVERTER : sqlExceptionConverter );
	}

    // SQLException ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Convert an SQLException using the current converter, doing some logging first.
     *
     * @param sqlException The exception to convert
     * @param message An error message.
     * @return The converted exception
     */
    public JDBCException convert( SQLException sqlException,
                                  String message ) {
        return convert(sqlException, message, "n/a");
    }

    /**
     * Convert an SQLException using the current converter, doing some logging first.
     *
     * @param sqlException The exception to convert
     * @param message An error message.
     * @param sql The SQL being executed when the exception occurred
     * @return The converted exception
     */
    public JDBCException convert( SQLException sqlException,
                                  String message,
                                  String sql ) {
        return sqlExceptionConverter.convert(sqlException, message, sql);
    }

    // SQLWarning ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Contract for handling {@link SQLWarning warnings}
     */
    public static interface WarningHandler {
        /**
         * Should processing be done? Allows short-circuiting if not.
         *
         * @return True to process warnings, false otherwise.
         */
        public boolean doProcess();

        /**
         * Prepare for processing of a {@link SQLWarning warning} stack.
         * <p/>
         * Note that the warning here is also the first passed to {@link #handleWarning}
         *
         * @param warning The first warning in the stack.
         */
        public void prepare( SQLWarning warning );

        /**
         * Handle an individual warning in the stack.
         *
         * @param warning The warning to handle.
         */
        public void handleWarning( SQLWarning warning );
    }

    /**
     * Basic support for {@link WarningHandler} implementations which log
     */
    public static abstract class WarningHandlerLoggingSupport implements WarningHandler {
        public final void handleWarning( SQLWarning warning ) {
        	StringBuilder buf = new StringBuilder(30).append("SQL Warning Code: ").append(warning.getErrorCode()).append(", SQLState: ").append(warning.getSQLState());
            logWarning(buf.toString(), warning.getMessage());
        }

        /**
         * Delegate to log common details of a {@link SQLWarning warning}
         *
         * @param description A description of the warning
         * @param message The warning message
         */
        protected abstract void logWarning( String description,
                                            String message );
    }

    public static class StandardWarningHandler extends WarningHandlerLoggingSupport {
        private final String introMessage;

        public StandardWarningHandler( String introMessage ) {
            this.introMessage = introMessage;
        }

		@Override
		public boolean doProcess() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void prepare(SQLWarning warning) {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void logWarning(String description, String message) {
			// TODO Auto-generated method stub
			
		}
    }

    public static StandardWarningHandler STANDARD_WARNING_HANDLER = new StandardWarningHandler(DEFAULT_WARNING_MSG);

    public void walkWarnings( SQLWarning warning,
                              WarningHandler handler ) {
        if (warning == null || handler.doProcess()) {
            return;
        }
        handler.prepare(warning);
        while (warning != null) {
            handler.handleWarning(warning);
            warning = warning.getNextWarning();
        }
    }

    /**
     * Standard (legacy) behavior for logging warnings associated with a JDBC {@link Connection} and clearing them.
     * <p/>
     * Calls {@link #handleAndClearWarnings(Connection, WarningHandler)} using {@link #STANDARD_WARNING_HANDLER}
     *
     * @param connection The JDBC connection potentially containing warnings
     */
    public void logAndClearWarnings( Connection connection ) {
        handleAndClearWarnings(connection, STANDARD_WARNING_HANDLER);
    }

    /**
     * General purpose handling of warnings associated with a JDBC {@link Connection}.
     *
     * @param connection The JDBC connection potentially containing warnings
     * @param handler The handler for each individual warning in the stack.
     * @see #walkWarnings
     */
    @SuppressWarnings( {"ThrowableResultOfMethodCallIgnored"} )
    public void handleAndClearWarnings( Connection connection,
                                        WarningHandler handler ) {
        try {
            walkWarnings(connection.getWarnings(), handler);
        } catch (SQLException sqle) {
        }
        try {
            // Sybase fail if we don't do that, sigh...
            connection.clearWarnings();
        } catch (SQLException sqle) {
        }
    }

    /**
     * General purpose handling of warnings associated with a JDBC {@link Statement}.
     *
     * @param statement The JDBC statement potentially containing warnings
     * @param handler The handler for each individual warning in the stack.
     * @see #walkWarnings
     */
    @SuppressWarnings( {"ThrowableResultOfMethodCallIgnored"} )
    public void handleAndClearWarnings( Statement statement,
                                        WarningHandler handler ) {
        try {
            walkWarnings(statement.getWarnings(), handler);
        } catch (SQLException sqlException) {
            // workaround for WebLogic
        }
        try {
            // Sybase fail if we don't do that, sigh...
            statement.clearWarnings();
        } catch (SQLException sqle) {
        }
    }

}
