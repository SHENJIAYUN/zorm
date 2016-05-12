package com.zorm.meta;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractTableSpecification implements TableSpecification{
	private final static AtomicInteger tableCounter = new AtomicInteger( 0 );
	private final int tableNumber;
	
	public AbstractTableSpecification() {
		this.tableNumber = tableCounter.getAndIncrement();
	}
	
	@Override
	public int getTableNumber() {
		return tableNumber;
	}
}
