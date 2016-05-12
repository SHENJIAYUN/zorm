package com.zorm.transaction;

import java.io.Serializable;

import com.zorm.engine.transaction.spi.TransactionImplementor;
import com.zorm.jdbc.JdbcCoordinator;

public interface TransactionCoordinator extends Serializable{

	public TransactionContext getTransactionContext();

	public JdbcCoordinator getJdbcCoordinator();

	public TransactionImplementor getTransaction();

	public boolean takeOwnership();

	public void sendAfterTransactionBeginNotifications(
			TransactionImplementor jdbcTransaction);

	public boolean isTransactionInProgress();

	public void sendBeforeTransactionCompletionNotifications(
			TransactionImplementor jdbcTransaction);

	public void afterTransaction(TransactionImplementor jdbcTransaction, int status);

	public void removeObserver(TransactionObserver transactionObserver);

}
