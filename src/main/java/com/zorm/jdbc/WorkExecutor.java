package com.zorm.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public class WorkExecutor<T> {
	public <T> T executeWork(Work work, Connection connection) throws SQLException {
		work.execute( connection );
		return null;
	}
	
	public <T> T executeReturningWork(ReturningWork<T> work, Connection connection) throws SQLException {
		return work.execute( connection );
	}
}
