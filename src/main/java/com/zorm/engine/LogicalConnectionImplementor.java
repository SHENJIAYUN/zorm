package com.zorm.engine;

import java.sql.Connection;

import com.zorm.jdbc.JdbcResourceRegistry;
import com.zorm.jdbc.JdbcServices;


public interface LogicalConnectionImplementor extends LogicalConnection{

	public void addObserver(ConnectionObserver observer);

	public JdbcServices getJdbcServices();

	public JdbcResourceRegistry getResourceRegistry();

	

}
