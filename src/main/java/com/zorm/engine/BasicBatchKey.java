package com.zorm.engine;

import com.zorm.jdbc.Expectation;

public class BasicBatchKey implements BatchKey{
	private final String comparison;
	private final int statementCount;
	private final Expectation expectation;

//	public BasicBatchKey(String comparison, int statementCount, Expectation expectation) {
//		this.comparison = comparison;
//		this.statementCount = statementCount;
//		this.expectations = new Expectation[statementCount];
//		Arrays.fill( this.expectations, expectation );
//	}
//
//	public BasicBatchKey(String comparison, Expectation... expectations) {
//		this.comparison = comparison;
//		this.statementCount = expectations.length;
//		this.expectations = expectations;
//	}

	public BasicBatchKey(String comparison, Expectation expectation) {
		this.comparison = comparison;
		this.statementCount = 1;
		this.expectation = expectation;
	}

	@Override
	public Expectation getExpectation() {
		return expectation;
	}

	@Override
	public int getBatchedStatementCount() {
		return statementCount;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		BasicBatchKey that = (BasicBatchKey) o;

		if ( !comparison.equals( that.comparison ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return comparison.hashCode();
	}
}
