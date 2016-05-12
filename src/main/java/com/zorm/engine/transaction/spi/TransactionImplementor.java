package com.zorm.engine.transaction.spi;

import com.zorm.transaction.Transaction;

public interface TransactionImplementor extends Transaction{

	public void invalidate();

	public void markForJoin();

	public void join();

	public JoinStatus getJoinStatus();
	
	public void resetJoinStatus();

	public void markRollbackOnly();


}
