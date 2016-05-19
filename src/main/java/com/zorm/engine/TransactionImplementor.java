package com.zorm.engine;

import com.zorm.transaction.Transaction;

public interface TransactionImplementor extends Transaction{

	public void invalidate();

	public void markForJoin();

	public void join();

	public JoinStatus getJoinStatus();
	
	public void resetJoinStatus();

	public void markRollbackOnly();


}
