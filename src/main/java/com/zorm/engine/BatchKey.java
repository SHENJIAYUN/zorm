package com.zorm.engine;

import com.zorm.jdbc.Expectation;

public interface BatchKey {
	public int getBatchedStatementCount();

	/**
	 * Get the expectation pertaining to the outcome of the {@link Batch} associated with this key.
	 *
	 * @return The expectations
	 */
	public Expectation getExpectation();
}
