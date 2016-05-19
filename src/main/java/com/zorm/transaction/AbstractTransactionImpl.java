package com.zorm.transaction;

import javax.transaction.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.engine.JoinStatus;
import com.zorm.engine.LocalStatus;
import com.zorm.engine.TransactionImplementor;
import com.zorm.exception.TransactionException;
import com.zorm.exception.ZormException;
import com.zorm.service.JtaPlatform;

public abstract class AbstractTransactionImpl implements TransactionImplementor{
	
	private static final Log log = LogFactory.getLog(AbstractTransactionImpl.class);
	
	private final TransactionCoordinator transactionCoordinator;

	private boolean valid = true;

	private LocalStatus localStatus = LocalStatus.NOT_ACTIVE;
	private int timeout = -1;

	protected AbstractTransactionImpl(TransactionCoordinator transactionCoordinator) {
		this.transactionCoordinator = transactionCoordinator;
	}

	@Override
	public void invalidate() {
		valid = false;
	}

	/**
	 * Perform the actual steps of beginning a transaction according to the strategy.
	 *
	 * @throws org.hibernate.TransactionException Indicates a problem beginning the transaction
	 */
	protected abstract void doBegin();

	/**
	 * Perform the actual steps of committing a transaction according to the strategy.
	 *
	 * @throws org.hibernate.TransactionException Indicates a problem committing the transaction
	 */
	protected abstract void doCommit();

	/**
	 * Perform the actual steps of rolling back a transaction according to the strategy.
	 *
	 * @throws org.hibernate.TransactionException Indicates a problem rolling back the transaction
	 */
	protected abstract void doRollback();

	protected abstract void afterTransactionBegin();
	protected abstract void beforeTransactionCommit();
	protected abstract void beforeTransactionRollBack();
	protected abstract void afterTransactionCompletion(int status);
	protected abstract void afterAfterCompletion();

	/**
	 * Provide subclasses with access to the transaction coordinator.
	 *
	 * @return This transaction's context.
	 */
	protected TransactionCoordinator transactionCoordinator() {
		return transactionCoordinator;
	}

	/**
	 * Provide subclasses with convenient access to the configured {@link JtaPlatform}
	 *
	 * @return The {@link com.zorm.service.hibernate.service.jta.platform.spi.JtaPlatform}
	 */
	protected JtaPlatform jtaPlatform() {
		return transactionCoordinator().getTransactionContext().getTransactionEnvironment().getJtaPlatform();
	}


	@Override
	public LocalStatus getLocalStatus() {
		return localStatus;
	}

	@Override
	public boolean isActive() {
		return localStatus == LocalStatus.ACTIVE && doExtendedActiveCheck();
	}
	

	@Override
	public boolean isParticipating() {
		return getJoinStatus() == JoinStatus.JOINED && isActive();
	}

	@Override
	public boolean wasCommitted() {
		return localStatus == LocalStatus.COMMITTED;
	}

	@Override
	public boolean wasRolledBack() throws ZormException {
		return localStatus == LocalStatus.ROLLED_BACK;
	}

	/**
	 * Active has been checked against local state.  Perform any needed checks against resource transactions.
	 *
	 * @return {@code true} if the extended active check checks out as well; false otherwise.
	 */
	protected boolean doExtendedActiveCheck() {
		return true;
	}

	@Override
	public void begin() throws ZormException {
		if ( ! valid ) {
			throw new TransactionException( "Transaction instance is no longer valid" );
		}
		if ( localStatus == LocalStatus.ACTIVE ) {
			throw new TransactionException( "nested transactions not supported" );
		}
		if ( localStatus != LocalStatus.NOT_ACTIVE ) {
			throw new TransactionException( "reuse of Transaction instances not supported" );
		}


		doBegin();

		localStatus = LocalStatus.ACTIVE;

		afterTransactionBegin();
	}

	@Override
	public void commit() throws ZormException {
		if ( localStatus != LocalStatus.ACTIVE ) {
			throw new TransactionException( "Transaction not successfully started" );
		}

        log.info("begin transaction commit");
		beforeTransactionCommit();

		try {
			doCommit();
			localStatus = LocalStatus.COMMITTED;
			afterTransactionCompletion( Status.STATUS_COMMITTED );
		}
		catch ( Exception e ) {
			localStatus = LocalStatus.FAILED_COMMIT;
			afterTransactionCompletion( Status.STATUS_UNKNOWN );
			throw new TransactionException( "commit failed", e );
		}
		finally {
			invalidate();
			afterAfterCompletion();
		}
	}

	protected boolean allowFailedCommitToPhysicallyRollback() {
		return false;
	}

	@Override
	public void rollback() throws ZormException {
		if ( localStatus != LocalStatus.ACTIVE && localStatus != LocalStatus.FAILED_COMMIT ) {
			throw new TransactionException( "Transaction not successfully started" );
		}

		log.info("rolling back");

		beforeTransactionRollBack();

		if ( localStatus != LocalStatus.FAILED_COMMIT || allowFailedCommitToPhysicallyRollback() ) {
			try {
				doRollback();
				localStatus = LocalStatus.ROLLED_BACK;
				afterTransactionCompletion( Status.STATUS_ROLLEDBACK );
			}
			catch ( Exception e ) {
				afterTransactionCompletion( Status.STATUS_UNKNOWN );
				throw new TransactionException( "rollback failed", e );
			}
			finally {
				invalidate();
				afterAfterCompletion();
			}
		}

	}

	@Override
	public void setTimeout(int seconds) {
		timeout = seconds;
	}

	@Override
	public int getTimeout() {
		return timeout;
	}

	@Override
	public void markForJoin() {
	}

	@Override
	public void join() {
		// generally speaking this is no-op
	}

	@Override
	public void resetJoinStatus() {
		// generally speaking this is no-op
	}
}
