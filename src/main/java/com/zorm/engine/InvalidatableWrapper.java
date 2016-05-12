package com.zorm.engine;

public interface InvalidatableWrapper<T> extends JdbcWrapper<T> {
	public void invalidate();
}
