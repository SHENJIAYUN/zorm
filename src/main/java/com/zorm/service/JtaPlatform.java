package com.zorm.service;

import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

public interface JtaPlatform extends Service{
	/**
	 * Locate the {@link TransactionManager}
	 *
	 * @return The {@link TransactionManager}
	 */
	public TransactionManager retrieveTransactionManager();

	/**
	 * Locate the {@link UserTransaction}
	 *
	 * @return The {@link UserTransaction}
	 */
	public UserTransaction retrieveUserTransaction();

	/**
	 * Determine an identifier for the given transaction appropriate for use in caching/lookup usages.
	 * <p/>
	 * Generally speaking the transaction itself will be returned here.  This method was added specifically
	 * for use in WebSphere and other unfriendly JEE containers (although WebSphere is still the only known
	 * such brain-dead, sales-driven impl).
	 *
	 * @param transaction The transaction to be identified.
	 * @return An appropriate identifier
	 */
	public Object getTransactionIdentifier(Transaction transaction);

	/**
	 * Can we currently register a {@link Synchronization}?
	 *
	 * @return True if registering a {@link Synchronization} is currently allowed; false otherwise.
	 */
	public boolean canRegisterSynchronization();

	/**
	 * Register a JTA {@link Synchronization} in the means defined by the platform.
	 *
	 * @param synchronization The synchronization to register
	 */
	public void registerSynchronization(Synchronization synchronization);

	/**
	 * Obtain the current transaction status using whatever means is preferred for this platform
	 *
	 * @return The current status.
	 *
	 * @throws SystemException Indicates a problem access the underlying status
	 */
	public int getCurrentStatus() throws SystemException;
}
