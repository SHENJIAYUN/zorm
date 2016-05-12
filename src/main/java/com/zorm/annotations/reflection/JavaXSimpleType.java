package com.zorm.annotations.reflection;

import java.lang.reflect.Type;
import java.util.Collection;

public class JavaXSimpleType extends JavaXType {

	public JavaXSimpleType(Type type,TypeEnvironment context,JavaReflectionManager factory) {
          super(type,context,factory);
	}
	
	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public XClass getElementClass() {
		return toXClass(approximate());
	}

	@Override
	public XClass getClassOrElementClass() {
		return getElementClass();
	}

	@Override
	public Class<? extends Collection> getCollectionClass() {
		return null;
	}

	@Override
	public XClass getMapKey() {
		return null;
	}

	@Override
	public XClass getType() {
		return toXClass(approximate());
	}

}
