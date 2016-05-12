package com.zorm.jdbc;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public interface JdbcResourceRegistry extends Serializable{

	public void register(ResultSet resultSet);

	public boolean hasRegisteredResources();

	public void releaseResources();

	public void release(ResultSet proxy);

	public void register(Statement statement);

	public void registerLastQuery(Statement statement);

	public void close();

}
