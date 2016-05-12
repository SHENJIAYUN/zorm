package com.zorm.engine;

import java.sql.PreparedStatement;

import com.zorm.exception.ZormException;
import com.zorm.jdbc.JdbcCoordinator;

public class BatchingBatch extends AbstractBatchImpl{
	private final int batchSize;
	private int batchPosition;
	private int statementPosition;

	public BatchingBatch(
			BatchKey key,
			JdbcCoordinator jdbcCoordinator,
			int batchSize) {
		super( key, jdbcCoordinator );
		if ( ! key.getExpectation().canBeBatched() ) {
			throw new ZormException( "attempting to batch an operation which cannot be batched" );
		}
		this.batchSize = batchSize;
	}



	@Override
	public void addToBatch() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void execute() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doExecuteBatch() {
		// TODO Auto-generated method stub
		
	}
}
