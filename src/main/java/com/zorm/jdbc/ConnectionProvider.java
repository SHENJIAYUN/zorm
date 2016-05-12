package com.zorm.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import com.zorm.service.Service;
import com.zorm.service.Wrapped;

/**
 * 获取JDBC连接的接口
 * @author JIA
 *
 */
public interface ConnectionProvider extends Service,Wrapped{
    //获取JDBC连接
	public Connection getConnection() throws SQLException;
	//释放JDBC连接
	public void closeConnection(Connection conn) throws SQLException;
	
	public boolean supportsAggressiveRelease();
}
