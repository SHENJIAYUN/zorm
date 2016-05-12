package com.zorm.exception;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.zorm.util.JdbcExceptionHelper;

public class SQLStateConversionDelegate extends AbstractSQLExceptionConversionDelegate {

	private static final Set<String> SQL_GRAMMAR_CATEGORIES = buildGrammarCategories();
	private static Set<String> buildGrammarCategories() {
		HashSet<String> categories = new HashSet<String>();
		categories.addAll(
				Arrays.asList(
						"07", 	// "dynamic SQL error"
						"20",
						"2A", 	// "direct SQL syntax error or access rule violation"
						"37",	// "dynamic SQL syntax error or access rule violation"
						"42",	// "syntax error or access rule violation"
						"65",	// Oracle specific as far as I can tell
						"S0"	// MySQL specific as far as I can tell
				)
		);
		return Collections.unmodifiableSet( categories );
	}

	private static final Set DATA_CATEGORIES = buildDataCategories();
	private static Set<String> buildDataCategories() {
		HashSet<String> categories = new HashSet<String>();
		categories.addAll( 
				Arrays.asList(
						"21",	// "cardinality violation"
						"22"	// "data exception"
				)
		);
		return Collections.unmodifiableSet( categories );
	}

	private static final Set INTEGRITY_VIOLATION_CATEGORIES = buildContraintCategories();
	private static Set<String> buildContraintCategories() {
		HashSet<String> categories = new HashSet<String>();
		categories.addAll(
				Arrays.asList(
						"23",	// "integrity constraint violation"
						"27",	// "triggered data change violation"
						"44"	// "with check option violation"
				)
		);
		return Collections.unmodifiableSet( categories );
	}

	private static final Set CONNECTION_CATEGORIES = buildConnectionCategories();
	private static Set<String> buildConnectionCategories() {
		HashSet<String> categories = new HashSet<String>();
		categories.add(
				"08"	// "connection exception"
		);
		return Collections.unmodifiableSet( categories );
	}

	public SQLStateConversionDelegate(ConversionContext conversionContext) {
		super( conversionContext );
	}

	@Override
	public JDBCException convert(SQLException sqlException, String message, String sql) {
		String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );

		if ( sqlState != null ) {
			String sqlStateClassCode = JdbcExceptionHelper.determineSqlStateClassCode( sqlState );

			if ( sqlStateClassCode != null ) {
				if ( SQL_GRAMMAR_CATEGORIES.contains( sqlStateClassCode ) ) {
					return new SQLGrammarException( message, sqlException, sql );
				}
				else if ( INTEGRITY_VIOLATION_CATEGORIES.contains( sqlStateClassCode ) ) {
					final String constraintName = getConversionContext()
							.getViolatedConstraintNameExtracter()
							.extractConstraintName( sqlException );
					return new ConstraintViolationException( message, sqlException, sql, constraintName );
				}
				else if ( CONNECTION_CATEGORIES.contains( sqlStateClassCode ) ) {
					return new JDBCConnectionException( message, sqlException, sql );
				}
				else if ( DATA_CATEGORIES.contains( sqlStateClassCode ) ) {
					return new DataException( message, sqlException, sql );
				}
			}

			if ( "40001".equals( sqlState ) ) {
				return new LockAcquisitionException( message, sqlException, sql );
			}

			if ( "40XL1".equals( sqlState ) || "40XL2".equals( sqlState )) {
				// Derby "A lock could not be obtained within the time requested."
				return new PessimisticLockException( message, sqlException, sql );
			}

			// MySQL Query execution was interrupted
			if ( "70100".equals( sqlState ) ||
				// Oracle user requested cancel of current operation
				  "72000".equals( sqlState ) ) {
				throw new QueryTimeoutException(  message, sqlException, sql );
			}
		}

		return null;
	}
}

