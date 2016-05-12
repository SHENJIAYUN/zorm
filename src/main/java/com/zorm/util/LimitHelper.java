package com.zorm.util;

import com.zorm.dialect.LimitHandler;
import com.zorm.engine.RowSelection;

public class LimitHelper {

	public static boolean hasFirstRow(RowSelection selection) {
		return getFirstRow( selection ) > 0;
	}

	public static int getFirstRow(RowSelection selection) {
		return ( selection == null || selection.getFirstRow() == null ) ? 0 : selection.getFirstRow();
	}

	public static boolean hasMaxRows(RowSelection selection) {
		return selection != null && selection.getMaxRows() != null && selection.getMaxRows() > 0;
	}

	public static boolean useLimit(LimitHandler limitHandler,
			RowSelection rowSelection) {
		return limitHandler.supportsLimit() && hasMaxRows( rowSelection );
	}
}