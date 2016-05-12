package com.zorm.service;

public interface Wrapped {
	public boolean isUnwrappableAs(Class unwrapType);
	public <T> T unwrap(Class<T> unwrapType);
}
