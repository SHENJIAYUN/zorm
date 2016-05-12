package com.zorm.annotations.reflection;

import java.lang.reflect.Type;
import java.util.Collection;

abstract class JavaXType {
	private final TypeEnvironment context;
	private final JavaReflectionManager factory;
	private final Type approximatedType;
	private final Type boundType;
	
	protected JavaXType(Type unboundType, TypeEnvironment context, JavaReflectionManager factory) {
		this.context = context;
		this.factory = factory;
		this.boundType = context.bind( unboundType );
		this.approximatedType = factory.toApproximatingEnvironment( context ).bind( unboundType );
	}
	
	abstract public boolean isArray();

	abstract public boolean isCollection();

	abstract public XClass getElementClass();

	abstract public XClass getClassOrElementClass();

	abstract public Class<? extends Collection> getCollectionClass();

	abstract public XClass getMapKey();

	abstract public XClass getType();
	
	public boolean isResolved() {
		return TypeUtils.isResolved( boundType );
	}

	protected Type approximate() {
		return approximatedType;
	}

	protected XClass toXClass(Type type) {
		return factory.toXClass( type, context );
	}
}
