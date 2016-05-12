package com.zorm.annotations.reflection;

import java.lang.reflect.Type;

public interface TypeEnvironment {

	public Type bind(Type type);
}
