package com.zorm.service;

import com.zorm.ConnectionReleaseMode;
import com.zorm.engine.TransactionImplementor;
import com.zorm.transaction.TransactionCoordinator;

/*
 * 新建事务
 */
public interface TransactionFactory<T extends TransactionImplementor> extends Service {

	public T createTransaction(
			TransactionCoordinator transactionCoordinatorImpl);

	public boolean compatibleWithJtaSynchronization();
	
	public boolean canBeDriver();
	
	public boolean isJoinableJtaTransaction(TransactionCoordinator transactionCoordinator, T transaction);
	
	public ConnectionReleaseMode getDefaultReleaseMode();
}
