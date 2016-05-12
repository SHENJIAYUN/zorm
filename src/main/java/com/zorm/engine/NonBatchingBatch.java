package com.zorm.engine;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import com.zorm.jdbc.JdbcCoordinator;

public class NonBatchingBatch extends AbstractBatchImpl {
	protected NonBatchingBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
		super( key, jdbcCoordinator );
	}



	@Override
	public void addToBatch() {
		//notifyObserversImplicitExecution();
		for ( Map.Entry<String,PreparedStatement> entry : getStatements().entrySet() ) {
			try {
				final PreparedStatement statement = entry.getValue();
				final int rowCount = statement.executeUpdate();
				getKey().getExpectation().verifyOutcome( rowCount, statement, 0 );
				try {
					statement.close();
				}
				catch (SQLException e) {
				}
			}
			catch ( SQLException e ) {
				throw sqlExceptionHelper().convert( e, "could not execute batch statement", entry.getKey() );
			}
		}
		getStatements().clear();
		
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
