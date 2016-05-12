package com.zorm.meta;

public abstract class AbstractSimpleValue implements SimpleValue{
	private final TableSpecification table;
	private final int position;
	private Datatype datatype;

	protected AbstractSimpleValue(TableSpecification table, int position) {
		this.table = table;
		this.position = position;
	}
	
	public int getPosition() {
		return position;
	}
	
	@Override
	public TableSpecification getTable() {
		return table;
	}
}
