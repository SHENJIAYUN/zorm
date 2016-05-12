package com.zorm.engine;

import java.io.Serializable;
import java.sql.Connection;

public interface LogicalConnection extends Serializable{
	public Connection getConnection();
	
	public Connection getShareableConnectionProxy();

	public Connection close();
}
