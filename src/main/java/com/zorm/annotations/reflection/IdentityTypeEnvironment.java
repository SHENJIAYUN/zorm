package com.zorm.annotations.reflection;

import java.lang.reflect.Type;

public class IdentityTypeEnvironment implements TypeEnvironment{

	public static final TypeEnvironment INSTANCE = new IdentityTypeEnvironment();
	
	private IdentityTypeEnvironment(){}
	
	public Type bind(Type type) {
		return type;
	}
	
	public String toString(){
		return "{}";
	}

}
