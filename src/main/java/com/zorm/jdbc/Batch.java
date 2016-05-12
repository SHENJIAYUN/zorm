package com.zorm.jdbc;

import java.sql.PreparedStatement;

import com.zorm.engine.BatchKey;

public interface Batch {

	public BatchKey getKey();
	//public void addObserver(BatchObserver observer);

	public PreparedStatement getBatchStatement(String sql, boolean callable);

	public void addToBatch();

	/**
	 * Execute this batch.
	 */
	public void execute();

	/**
	 * Used to indicate that the batch instance is no longer needed and that, therefore, it can release its
	 * resources.
	 */
	public void release();
}
