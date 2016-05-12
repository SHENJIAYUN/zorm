package com.zorm.dialect;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.zorm.engine.RowSelection;
import com.zorm.util.LimitHelper;

public class NoopLimitHandler extends AbstractLimitHandler {

	public NoopLimitHandler(String sql, RowSelection selection) {
		super( sql, selection );
	}

	public String getProcessedSql() {
		return sql;
	}

	public int bindLimitParametersAtStartOfQuery(PreparedStatement statement, int index) {
		return 0;
	}
	
	

	public int bindLimitParametersAtEndOfQuery(PreparedStatement statement, int index) {
		return 0;
	}

	public void setMaxRows(PreparedStatement statement) throws SQLException {
		if ( LimitHelper.hasMaxRows( selection ) ) {
			statement.setMaxRows( selection.getMaxRows() + convertToFirstRowValue( LimitHelper.getFirstRow( selection ) ) );
		}
	}
}
