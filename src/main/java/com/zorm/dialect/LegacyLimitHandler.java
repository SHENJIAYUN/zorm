package com.zorm.dialect;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.zorm.engine.RowSelection;
import com.zorm.util.LimitHelper;

public class LegacyLimitHandler extends AbstractLimitHandler  {
	private final Dialect dialect;

	public LegacyLimitHandler(Dialect dialect, String sql, RowSelection selection) {
		super( sql, selection );
		this.dialect = dialect;
	}

	@Override
	public boolean supportsLimit() {
		return dialect.supportsLimit();
	}

	public boolean supportsLimitOffset() {
		return dialect.supportsLimitOffset();
	}

	@Override
	public String getProcessedSql() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMaxRows(PreparedStatement st) throws SQLException {
		// TODO Auto-generated method stub
		
	}
	
}
