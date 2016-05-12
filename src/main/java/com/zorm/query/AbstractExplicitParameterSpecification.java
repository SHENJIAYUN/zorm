package com.zorm.query;

import com.zorm.type.Type;

public abstract class AbstractExplicitParameterSpecification implements ExplicitParameterSpecification {
	private final int sourceLine;
	private final int sourceColumn;
	private Type expectedType;

	/**
	 * Constructs an AbstractExplicitParameterSpecification.
	 *
	 * @param sourceLine See {@link #getSourceLine()}
	 * @param sourceColumn See {@link #getSourceColumn()} 
	 */
	protected AbstractExplicitParameterSpecification(int sourceLine, int sourceColumn) {
		this.sourceLine = sourceLine;
		this.sourceColumn = sourceColumn;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getSourceLine() {
		return sourceLine;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getSourceColumn() {
		return sourceColumn;
	}

	/**
	 * {@inheritDoc}
	 */
	public Type getExpectedType() {
		return expectedType;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setExpectedType(Type expectedType) {
		this.expectedType = expectedType;
	}
}