package com.zorm.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class AbstractWork implements Work,WorkExecutorVisitable<Void>{
	public Void accept(WorkExecutor<Void> executor, Connection connection) throws SQLException {
		return executor.executeWork( this, connection );
	}
}
