package com.zorm.type;

import com.zorm.typet.TypeHelper;

public class TypeLocatorImpl implements TypeHelper {

	private final TypeResolver typeResolver;

	public TypeLocatorImpl(TypeResolver typeResolver) {
		this.typeResolver = typeResolver;
	}
	
	@Override
	public BasicType basic(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BasicType basic(Class javaType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type heuristicType(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type entity(Class entityClass) {
		// TODO Auto-generated method stub
		return null;
	}

}
