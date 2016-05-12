package com.zorm.config;

public class UniqueConstraintHolder {
	private String name;
	private String[] columns;

	public String getName() {
		return name;
	}

	public UniqueConstraintHolder setName(String name) {
		this.name = name;
		return this;
	}

	public String[] getColumns() {
		return columns;
	}

	public UniqueConstraintHolder setColumns(String[] columns) {
		this.columns = columns;
		return this;
	}
}

