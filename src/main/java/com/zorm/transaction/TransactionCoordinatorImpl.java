package com.zorm.transaction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import com.zorm.ConnectionReleaseMode;
import com.zorm.engine.SynchronizationRegistryImpl;
import com.zorm.engine.transaction.spi.JoinStatus;
import com.zorm.engine.transaction.spi.TransactionImplementor;
import com.zorm.exception.ResourceClosedException;
import com.zorm.jdbc.JdbcCoordinator;
import com.zorm.jdbc.JdbcCoordinatorImpl;
import com.zorm.service.TransactionFactory;
import com.zorm.util.JtaStatusHelper;

public class TransactionCoordinatorImpl implements TransactionCoordinator{
	private final transient TransactionContext transactionContext;
	private final transient JdbcCoordinatorImpl jdbcCoordinator;
	private final transient TransactionFactory transactionFactory;
	private final transient TransactionEnvironment transactionEnvironment;

	private final transient List<TransactionObserver> observers;
	private final transient SynchronizationRegistryImpl synchronizationRegistry;

	private transient TransactionImplementor currentZormTransaction;

	//private transient SynchronizationCallbackCoordinatorImpl callbackCoordinator;

	private transient boolean open = true;
	private transient boolean synchronizationRegistered;
	private transient boolean ownershipTaken;

	public TransactionCoordinatorImpl(
			Connection userSuppliedConnection,
			TransactionContext transactionContext) {
		this.transactionContext = transactionContext;
		this.jdbcCoordinator = new JdbcCoordinatorImpl( userSuppliedConnection, this );
		this.transactionEnvironment = transactionContext.getTransactionEnvironment();
		this.transactionFactory = this.transactionEnvironment.getTransactionFactory();
		this.observers = new ArrayList<TransactionObserver>();
		this.synchronizationRegistry = new SynchronizationRegistryImpl();
		reset();

		final boolean registerSynchronization = transactionContext.isAutoCloseSessionEnabled()
		        || transactionContext.isFlushBeforeCompletionEnabled()
		        || transactionContext.getConnectionReleaseMode() == ConnectionReleaseMode.AFTER_TRANSACTION;
		if ( registerSynchronization ) {
			//pulse();
		}
	}

	public void reset() {
		synchronizationRegistered = false;
		ownershipTaken = false;
		if(currentZormTransaction != null){
			currentZormTransaction.invalidate();
		}
		currentZormTransaction = transactionFactory().createTransaction(this);
		if ( transactionContext.shouldAutoJoinTransaction() ) {
			currentZormTransaction.markForJoin();
			currentZormTransaction.join();
		}
		synchronizationRegistry.clearSynchronizations();
	}

	private TransactionFactory transactionFactory() {
		return transactionFactory;
	}

	public TransactionCoordinatorImpl(
			TransactionContext transactionContext,
			JdbcCoordinatorImpl jdbcCoordinator,
			List<TransactionObserver> observers) {
		this.transactionContext = transactionContext;
		this.jdbcCoordinator = jdbcCoordinator;
		this.transactionEnvironment = transactionContext.getTransactionEnvironment();
		this.transactionFactory = this.transactionEnvironment.getTransactionFactory();
		this.observers = observers;
		this.synchronizationRegistry = new SynchronizationRegistryImpl();
		reset();
	}

	@Override
	public boolean takeOwnership() {
		if ( ownershipTaken ) {
			return false;
		}
		else {
			ownershipTaken = true;
			return true;
		}
	}

	@Override
	public TransactionContext getTransactionContext() {
		return transactionContext;
	}

	@Override
	public JdbcCoordinator getJdbcCoordinator() {
		return jdbcCoordinator;
	}

	@Override
	public TransactionImplementor getTransaction() {
		if(!open){
			throw new ResourceClosedException("This TransactionCoordinator has been closed");
		}
		return currentZormTransaction;
	}

	@Override
	public void sendAfterTransactionBeginNotifications(
			TransactionImplementor jdbcTransaction) {
		for(TransactionObserver observer : observers){
			observer.afterBegin(currentZormTransaction);
		}
	}

	public void pulse() {
		if ( transactionFactory().compatibleWithJtaSynchronization() ) {
			// the configured transaction strategy says it supports callbacks via JTA synchronization, so attempt to
			// register JTA synchronization if possible
			//attemptToRegisterJtaSync();
		}
	}

	@Override
	public boolean isTransactionInProgress() {
		return getTransaction().isActive() && getTransaction().getJoinStatus() == JoinStatus.JOINED;
	}

	@Override
	public void sendBeforeTransactionCompletionNotifications(
			TransactionImplementor jdbcTransaction) {
		synchronizationRegistry.notifySynchronizationsBeforeTransactionCompletion();
		
	}

	@Override
	public void afterTransaction(TransactionImplementor jdbcTransaction,
			int status) {
		final boolean success = JtaStatusHelper.isCommitted( status );
		getJdbcCoordinator().afterTransaction();
		getTransactionContext().afterTransactionCompletion( jdbcTransaction, success );
	
	    reset();
	}

	public Connection close() {
		open = false;
		reset();
		observers.clear();
		return jdbcCoordinator.close();
	}

	@Override
	public void removeObserver(TransactionObserver observer) {
		observers.remove( observer );
	}

}
