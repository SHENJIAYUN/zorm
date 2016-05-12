package com.zorm.engine;

public interface JdbcWrapper<T> {
	public T getWrappedObject();
}
