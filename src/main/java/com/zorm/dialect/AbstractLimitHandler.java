package com.zorm.dialect;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.zorm.engine.RowSelection;
import com.zorm.util.LimitHelper;

public abstract class AbstractLimitHandler implements LimitHandler{
	protected final String sql;
	protected final RowSelection selection;
	
	public AbstractLimitHandler(String sql, RowSelection selection) {
		this.sql = sql;
		this.selection = selection;
	}
	
	public boolean supportsLimitOffset() {
		return supportsLimit();
	}
	
	public boolean bindLimitParametersFirst() {
		return false;
	}
	
	public boolean supportsVariableLimit() {
		return supportsLimit();
	}
	
	protected int getMaxOrLimit() {
		final int firstRow = convertToFirstRowValue( LimitHelper.getFirstRow( selection ) );
		final int lastRow = selection.getMaxRows();
		return useMaxForLimit() ? lastRow + firstRow : lastRow;
	}
	
	public int bindLimitParametersAtEndOfQuery(PreparedStatement statement, int index)
			throws SQLException {
		return !bindLimitParametersFirst() ? bindLimitParameters( statement, index ) : 0;
	}
	
	public boolean useMaxForLimit() {
		return false;
	}
	
	public boolean forceLimitUsage() {
		return false;
	}
	
	public boolean bindLimitParametersInReverseOrder() {
		return false;
	}
	
	protected int bindLimitParameters(PreparedStatement statement, int index)
			throws SQLException {
		if ( !supportsVariableLimit() || !LimitHelper.hasMaxRows( selection ) ) {
			return 0;
		}
		int firstRow = convertToFirstRowValue( LimitHelper.getFirstRow( selection ) );
		int lastRow = getMaxOrLimit();
		boolean hasFirstRow = supportsLimitOffset() && ( firstRow > 0 || forceLimitUsage() );
		boolean reverse = bindLimitParametersInReverseOrder();
		if ( hasFirstRow ) {
			statement.setInt( index + ( reverse ? 1 : 0 ), firstRow );
		}
		statement.setInt( index + ( reverse || !hasFirstRow ? 0 : 1 ), lastRow );
		return hasFirstRow ? 2 : 1;
	}
	
	public int bindLimitParametersAtStartOfQuery(PreparedStatement statement, int index)
			throws SQLException {
		return bindLimitParametersFirst() ? bindLimitParameters( statement, index ) : 0;
	}
	
	@Override
	public boolean supportsLimit() {
		return false;
	}
	
	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		return zeroBasedFirstResult;
	}
	
	
}

