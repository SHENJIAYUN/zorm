package com.zorm.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import com.zorm.engine.ExecuteUpdateResultCheckStyle;
import com.zorm.exception.BatchFailedException;
import com.zorm.exception.BatchedTooManyRowsAffectedException;
import com.zorm.exception.GenericJDBCException;
import com.zorm.exception.StaleStateException;
import com.zorm.exception.TooManyRowsAffectedException;
import com.zorm.exception.ZormException;

public class Expectations {

	private static SqlExceptionHelper sqlExceptionHelper = new SqlExceptionHelper();

	public static final int USUAL_EXPECTED_COUNT = 1;
	public static final int USUAL_PARAM_POSITION = 1;


	// Base Expectation impls ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static class BasicExpectation implements Expectation {
		private final int expectedRowCount;

		protected BasicExpectation(int expectedRowCount) {
			this.expectedRowCount = expectedRowCount;
			if ( expectedRowCount < 0 ) {
				throw new IllegalArgumentException( "Expected row count must be greater than zero" );
			}
		}

		public final void verifyOutcome(int rowCount, PreparedStatement statement, int batchPosition) {
			rowCount = determineRowCount( rowCount, statement );
			if ( batchPosition < 0 ) {
				checkNonBatched( rowCount );
			}
			else {
				checkBatched( rowCount, batchPosition );
			}
		}

		private void checkBatched(int rowCount, int batchPosition) {
            if (rowCount == -2) {}
            else if (rowCount == -3) throw new BatchFailedException("Batch update failed: " + batchPosition);
			else {
                if (expectedRowCount > rowCount) throw new StaleStateException(
                                                                               "Batch update returned unexpected row count from update ["
                                                                               + batchPosition + "]; actual row count: " + rowCount
                                                                               + "; expected: " + expectedRowCount);
				if ( expectedRowCount < rowCount ) {
					String msg = "Batch update returned unexpected row count from update [" +
					             batchPosition + "]; actual row count: " + rowCount +
					             "; expected: " + expectedRowCount;
					throw new BatchedTooManyRowsAffectedException( msg, expectedRowCount, rowCount, batchPosition );
				}
			}
		}

		private void checkNonBatched(int rowCount) {
			if ( expectedRowCount > rowCount ) {
				throw new StaleStateException(
						"Unexpected row count: " + rowCount + "; expected: " + expectedRowCount
				);
			}
			if ( expectedRowCount < rowCount ) {
				String msg = "Unexpected row count: " + rowCount + "; expected: " + expectedRowCount;
				throw new TooManyRowsAffectedException( msg, expectedRowCount, rowCount );
			}
		}

		public int prepare(PreparedStatement statement) throws SQLException, ZormException {
			return 0;
		}

		public boolean canBeBatched() {
			return true;
		}

		protected int determineRowCount(int reportedRowCount, PreparedStatement statement) {
			return reportedRowCount;
		}
	}

	public static class BasicParamExpectation extends BasicExpectation {
		private final int parameterPosition;
		protected BasicParamExpectation(int expectedRowCount, int parameterPosition) {
			super( expectedRowCount );
			this.parameterPosition = parameterPosition;
		}

		@Override
        public int prepare(PreparedStatement statement) throws SQLException, ZormException {
			toCallableStatement( statement ).registerOutParameter( parameterPosition, Types.NUMERIC );
			return 1;
		}

		@Override
        public boolean canBeBatched() {
			return false;
		}

		@Override
        protected int determineRowCount(int reportedRowCount, PreparedStatement statement) {
			try {
				return toCallableStatement( statement ).getInt( parameterPosition );
			}
			catch( SQLException sqle ) {
				//sqlExceptionHelper.logExceptions( sqle, "could not extract row counts from CallableStatement" );
				throw new GenericJDBCException( "could not extract row counts from CallableStatement", sqle );
			}
		}

		private CallableStatement toCallableStatement(PreparedStatement statement) {
			if ( ! CallableStatement.class.isInstance( statement ) ) {
				throw new ZormException( "BasicParamExpectation operates exclusively on CallableStatements : " + statement.getClass() );
			}
			return ( CallableStatement ) statement;
		}
	}


	// Various Expectation instances ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static final Expectation NONE = new Expectation() {
		public void verifyOutcome(int rowCount, PreparedStatement statement, int batchPosition) {
			
		}

		public int prepare(PreparedStatement statement) {
			return 0;
		}

		public boolean canBeBatched() {
			return true;
		}
	};

	public static final Expectation BASIC = new BasicExpectation( USUAL_EXPECTED_COUNT );

	public static final Expectation PARAM = new BasicParamExpectation( USUAL_EXPECTED_COUNT, USUAL_PARAM_POSITION );


	public static Expectation appropriateExpectation(ExecuteUpdateResultCheckStyle style) {
		if ( style == ExecuteUpdateResultCheckStyle.NONE ) {
			return NONE;
		}
		else if ( style == ExecuteUpdateResultCheckStyle.COUNT ) {
			return BASIC;
		}
		else if ( style == ExecuteUpdateResultCheckStyle.PARAM ) {
			return PARAM;
		}
		else {
			throw new ZormException( "unknown check style : " + style );
		}
	}

	private Expectations() {
	}
}
