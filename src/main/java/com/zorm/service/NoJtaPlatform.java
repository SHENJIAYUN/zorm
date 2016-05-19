package com.zorm.service;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

public class NoJtaPlatform implements JtaPlatform {

	@Override
	public TransactionManager retrieveTransactionManager() {
		return null;
	}

	@Override
	public UserTransaction retrieveUserTransaction() {
		return null;
	}

	@Override
	public Object getTransactionIdentifier(Transaction transaction) {
		return null;
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
	}

	@Override
	public boolean canRegisterSynchronization() {
		return false;
	}

	@Override
	public int getCurrentStatus() throws SystemException {
		return Status.STATUS_UNKNOWN;
	}

}
