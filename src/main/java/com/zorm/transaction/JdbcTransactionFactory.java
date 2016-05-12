package com.zorm.transaction;

import com.zorm.ConnectionReleaseMode;
import com.zorm.service.TransactionFactory;

public class JdbcTransactionFactory implements TransactionFactory<JdbcTransaction>{
	@Override
	public JdbcTransaction createTransaction(TransactionCoordinator transactionCoordinator) {
		return new JdbcTransaction( transactionCoordinator );
	}

	@Override
	public boolean compatibleWithJtaSynchronization() {
		return false;
	}

	@Override
	public boolean canBeDriver() {
		return true;
	}

	@Override
	public boolean isJoinableJtaTransaction(
			TransactionCoordinator transactionCoordinator,
			JdbcTransaction transaction) {
		return false;
	}

	@Override
	public ConnectionReleaseMode getDefaultReleaseMode() {
		return ConnectionReleaseMode.ON_CLOSE;
	}
}
