package com.zorm.type;


public interface TypeHelper {
	public BasicType basic(String name);
	public BasicType basic(Class javaType);
	public Type heuristicType(String name);
	public Type entity(Class entityClass);
}
