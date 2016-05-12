package com.zorm.jdbc;

import java.sql.Connection;

import com.zorm.engine.BatchBuilder;
import com.zorm.engine.BatchKey;
import com.zorm.engine.LogicalConnectionImpl;
import com.zorm.engine.LogicalConnectionImplementor;
import com.zorm.exception.TransactionException;
import com.zorm.exception.ZormException;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.transaction.TransactionCoordinator;
import com.zorm.transaction.TransactionCoordinatorImpl;
import com.zorm.transaction.TransactionEnvironment;

public class JdbcCoordinatorImpl implements JdbcCoordinator{
	private transient TransactionCoordinatorImpl transactionCoordinator;
	private final transient LogicalConnectionImpl logicalConnection;

	private transient Batch currentBatch;

	private transient long transactionTimeOutInstant = -1;
	private int flushDepth = 0;
	
	public JdbcCoordinatorImpl(
			Connection userSuppliedConnection,
			TransactionCoordinatorImpl transactionCoordinator) {
		this.transactionCoordinator = transactionCoordinator;
		this.logicalConnection = new LogicalConnectionImpl(
				userSuppliedConnection,
				transactionCoordinator.getTransactionContext().getConnectionReleaseMode(),
				transactionCoordinator.getTransactionContext().getTransactionEnvironment().getJdbcServices(),
				transactionCoordinator.getTransactionContext().getJdbcConnectionAccess()
		);
	}
	
	private JdbcCoordinatorImpl(LogicalConnectionImpl logicalConnection) {
		this.logicalConnection = logicalConnection;
	}
	
	@Override
	public void setTransactionTimeOut(int seconds) {
		transactionTimeOutInstant = System.currentTimeMillis() + ( seconds * 1000 );
	}
	
	@Override
	public LogicalConnectionImplementor getLogicalConnection() {
		return logicalConnection;
	}

	@Override
	public void flushBeginning() {
		if(flushDepth==0){
			logicalConnection.disableReleases();
		}
		flushDepth++;
	}

	@Override
	public Batch getBatch(BatchKey key) {
		if ( currentBatch != null ) {
			if ( currentBatch.getKey().equals( key ) ) {
				return currentBatch;
			}
			else {
				currentBatch.execute();
				currentBatch.release();
			}
		}
		currentBatch = batchBuilder().buildBatch( key, this );
		return currentBatch;
	}
	
	protected BatchBuilder batchBuilder() {
		return sessionFactory().getServiceRegistry().getService( BatchBuilder.class );
	}
	
	protected SessionFactoryImplementor sessionFactory() {
		return transactionEnvironment().getSessionFactory();
	}
	
	protected TransactionEnvironment transactionEnvironment() {
		return getTransactionCoordinator().getTransactionContext().getTransactionEnvironment();
	}
	
	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		return transactionCoordinator;
	}

	private transient StatementPreparer statementPreparer;
	
	@Override
	public StatementPreparer getStatementPreparer() {
		if ( statementPreparer == null ) {
			statementPreparer = new StatementPreparerImpl( this );
		}
		return statementPreparer;
	}

	public void executeBatch() {
		if(currentBatch != null){
			currentBatch.execute();
			currentBatch.release();
		}
	}

	public int determineRemainingTransactionTimeOutPeriod() {
		if ( transactionTimeOutInstant < 0 ) {
			return -1;
		}
		final int secondsRemaining = (int) ((transactionTimeOutInstant - System.currentTimeMillis()) / 1000);
		if ( secondsRemaining <= 0 ) {
			throw new TransactionException( "transaction timeout expired" );
		}
		return secondsRemaining;
	}

	@Override
	public void abortBatch() {
		flushDepth--;
		if ( flushDepth < 0 ) {
			throw new ZormException( "Mismatched flush handling" );
		}
		if ( flushDepth == 0 ) {
			logicalConnection.enableReleases();
		}
	}

	@Override
	public void flushEnding() {
		flushDepth--;
		if ( flushDepth < 0 ) {
			throw new ZormException( "Mismatched flush handling" );
		}
		if ( flushDepth == 0 ) {
			logicalConnection.enableReleases();
		}
	}

	@Override
	public void afterTransaction() {
		logicalConnection.afterTransaction();
		transactionTimeOutInstant = -1;
	}

	@Override
	public Connection close() {
		if ( currentBatch != null ) {
			currentBatch.release();
		}
		return logicalConnection.close();
	}
}
