package com.zorm.transaction;

import java.io.Serializable;

import com.zorm.ConnectionReleaseMode;
import com.zorm.engine.TransactionImplementor;
import com.zorm.jdbc.JdbcConnectionAccess;

public interface TransactionContext extends Serializable {

	public ConnectionReleaseMode getConnectionReleaseMode();

	public TransactionEnvironment getTransactionEnvironment();

	public JdbcConnectionAccess getJdbcConnectionAccess();

	public boolean shouldAutoJoinTransaction();

	public boolean isAutoCloseSessionEnabled();

	public boolean isFlushBeforeCompletionEnabled();

	public void afterTransactionBegin(TransactionImplementor jdbcTransaction);

	public boolean isFlushModeNever();

	public void managedFlush();

	public String onPrepareStatement(String sql);

	public void beforeTransactionCompletion(TransactionImplementor jdbcTransaction);

	public void afterTransactionCompletion(
			TransactionImplementor jdbcTransaction, boolean successful);

	public boolean shouldAutoClose();

	public boolean isClosed();

	public void managedClose();

}
