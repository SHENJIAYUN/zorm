package com.zorm.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.zorm.exception.ZormException;

public interface Expectation {
	/**
	 * Perform verification of the outcome of the RDBMS operation based on
	 * the type of expectation defined.
	 *
	 * @param rowCount The RDBMS reported "number of rows affected".
	 * @param statement The statement representing the operation
	 * @param batchPosition The position in the batch (if batching)
	 * @throws SQLException Exception from the JDBC driver
	 * @throws HibernateException Problem processing the outcome.
	 */
	public void verifyOutcome(int rowCount, PreparedStatement statement, int batchPosition) throws SQLException, ZormException;

	/**
	 * Perform any special statement preparation.
	 *
	 * @param statement The statement to be prepared
	 * @return The number of bind positions consumed (if any)
	 * @throws SQLException Exception from the JDBC driver
	 * @throws HibernateException Problem performing preparation.
	 */
	public int prepare(PreparedStatement statement) throws SQLException, ZormException;

	/**
	 * Is it acceptable to combiner this expectation with statement batching?
	 *
	 * @return True if batching can be combined with this expectation; false otherwise.
	 */
	public boolean canBeBatched();
}
