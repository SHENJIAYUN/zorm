package com.zorm.transaction;

import com.zorm.engine.TransactionImplementor;

public interface TransactionObserver {
	/**
	 * Callback for processing the beginning of a transaction.
	 *
	 * Do not rely on this being called as the transaction mat be started in a manner other than through the
	 * {@link org.hibernate.Transaction} API.
	 *
	 * @param transaction The Hibernate transaction
	 */
	public void afterBegin(TransactionImplementor transaction);

	/**
	 * Callback for processing the initial phase of transaction completion.
	 *
	 * @param transaction The Hibernate transaction
	 */
	public void beforeCompletion(TransactionImplementor transaction);

	/**
	 * Callback for processing the last phase of transaction completion.
	 *
	 * @param successful Was the transaction successful?
	 * @param transaction The Hibernate transaction
	 */
	public void afterCompletion(boolean successful, TransactionImplementor transaction);
}
