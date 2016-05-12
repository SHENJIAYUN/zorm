package com.zorm.typet;

import com.zorm.type.BasicType;
import com.zorm.type.Type;

public interface TypeHelper {
	public BasicType basic(String name);
	public BasicType basic(Class javaType);
	public Type heuristicType(String name);
	public Type entity(Class entityClass);
}
