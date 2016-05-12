package com.zorm.jdbc;

import java.sql.Connection;

import com.zorm.engine.BasicBatchKey;
import com.zorm.engine.BatchKey;
import com.zorm.engine.LogicalConnectionImplementor;
import com.zorm.transaction.TransactionCoordinator;

public interface JdbcCoordinator {

	public LogicalConnectionImplementor getLogicalConnection();

	public void setTransactionTimeOut(int seconds);

	public void flushBeginning();

	public Batch getBatch(BatchKey inserBatchKey);

	public TransactionCoordinator getTransactionCoordinator();

	public StatementPreparer getStatementPreparer();

	public void abortBatch();

	public void executeBatch();

	public void flushEnding();

	public void afterTransaction();

	public Connection close();

}
