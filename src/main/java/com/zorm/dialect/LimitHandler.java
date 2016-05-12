package com.zorm.dialect;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface LimitHandler {

	public boolean supportsLimit();

	public String getProcessedSql();

	public boolean supportsLimitOffset();

	public int bindLimitParametersAtStartOfQuery(PreparedStatement st, int col) throws SQLException;

	public int bindLimitParametersAtEndOfQuery(PreparedStatement st, int col) throws SQLException;

	public void setMaxRows(PreparedStatement st) throws SQLException;

}
