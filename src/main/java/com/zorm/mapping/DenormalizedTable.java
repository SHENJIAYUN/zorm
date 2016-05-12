package com.zorm.mapping;

import java.util.*;

import com.zorm.util.JoinedIterator;

public class DenormalizedTable extends Table {
	private static final long serialVersionUID = -2111832069414125039L;
	private final Table includedTable;
	
	public DenormalizedTable(Table includedTable) {
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}
	
	@Override
    public Column getColumn(Column column) {
		Column superColumn = super.getColumn( column );
		if (superColumn != null) {
			return superColumn;
		}
		else {
			return includedTable.getColumn( column );
		}
	}
	
	@Override
    public Iterator getColumnIterator() {
		return new JoinedIterator(
				includedTable.getColumnIterator(),
				super.getColumnIterator()
			);
	}
	
	@Override
    public boolean containsColumn(Column column) {
		return super.containsColumn(column) || includedTable.containsColumn(column);
	}
	
	@Override
    public PrimaryKey getPrimaryKey() {
		return includedTable.getPrimaryKey();
	}
}
