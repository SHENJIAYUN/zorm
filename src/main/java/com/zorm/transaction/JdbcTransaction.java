package com.zorm.transaction;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.engine.transaction.spi.JoinStatus;
import com.zorm.engine.transaction.spi.LocalStatus;
import com.zorm.exception.TransactionException;
import com.zorm.exception.ZormException;

public class JdbcTransaction extends AbstractTransactionImpl{
	
	private static final Log log = LogFactory.getLog(JdbcTransaction.class);
	
	private Connection managedConnection;
	private boolean wasInitiallyAutoCommit;
	private boolean isDriver;

	protected JdbcTransaction(TransactionCoordinator transactionCoordinator) {
		super( transactionCoordinator );
	}

	@Override
	protected void doBegin() {
		try {
			if ( managedConnection != null ) {
				throw new TransactionException( "Already have an associated managed connection" );
			}
			managedConnection = transactionCoordinator().getJdbcCoordinator().getLogicalConnection().getConnection();
			wasInitiallyAutoCommit = managedConnection.getAutoCommit();
			if ( wasInitiallyAutoCommit ) {
				managedConnection.setAutoCommit( false );
			}
		}
		catch( SQLException e ) {
			throw new TransactionException( "JDBC begin transaction failed: ", e );
		}

		isDriver = transactionCoordinator().takeOwnership();
	}

	@Override
	protected void afterTransactionBegin() {
		if ( getTimeout() > 0 ) {
			transactionCoordinator().getJdbcCoordinator().setTransactionTimeOut( getTimeout() );
		}
		transactionCoordinator().sendAfterTransactionBeginNotifications( this );
		if ( isDriver ) {
			transactionCoordinator().getTransactionContext().afterTransactionBegin( this );
		}
	}

	@Override
	protected void beforeTransactionCommit() {
		transactionCoordinator().sendBeforeTransactionCompletionNotifications( this );

		if ( isDriver && !transactionCoordinator().getTransactionContext().isFlushModeNever() ) {
			transactionCoordinator().getTransactionContext().managedFlush();
		}

		if ( isDriver ) {
			transactionCoordinator().getTransactionContext().beforeTransactionCompletion( this );
		}
	}

	@Override
	protected void doCommit() throws TransactionException {
		try {
			managedConnection.commit();
			log.debug("committed JDBC Connection");
		}
		catch( SQLException e ) {
			throw new TransactionException( "unable to commit against JDBC connection", e );
		}
		finally {
			releaseManagedConnection();
		}
	}

	private void releaseManagedConnection() {
		try {
			if ( wasInitiallyAutoCommit ) {
				log.debug("re-enabling autocommit");
				managedConnection.setAutoCommit( true );
			}
			managedConnection = null;
		}
		catch ( Exception e ) {
			log.debug("Could not toggle autocommit "+e);
		}
	}

	@Override
	protected void afterTransactionCompletion(int status) {
		transactionCoordinator().afterTransaction( this, status );
	}

	@Override
	protected void afterAfterCompletion() {
		if ( isDriver
				&& transactionCoordinator().getTransactionContext().shouldAutoClose()
				&& !transactionCoordinator().getTransactionContext().isClosed() ) {
			try {
				transactionCoordinator().getTransactionContext().managedClose();
			}
			catch (ZormException e) {
			}
		}
	}

	@Override
	protected void beforeTransactionRollBack() {
	}

	@Override
	protected void doRollback() throws TransactionException {
		try {
			managedConnection.rollback();
			log.info("rolled JDBC Connection");
		}
		catch( SQLException e ) {
			throw new TransactionException( "unable to rollback against JDBC connection", e );
		}
		finally {
			releaseManagedConnection();
		}
	}

	@Override
	public boolean isInitiator() {
		return isActive();
	}

	@Override
	public JoinStatus getJoinStatus() {
		return isActive() ? JoinStatus.JOINED : JoinStatus.NOT_JOINED;
	}

	@Override
	public void markRollbackOnly() {
		// nothing to do here
	}

	@Override
	public void join() {
		// nothing to do
	}

	@Override
	public void resetJoinStatus() {
		// nothing to do
	}

	@Override
	public boolean isActive() throws ZormException {
		return getLocalStatus() == LocalStatus.ACTIVE;
	}

	
}
