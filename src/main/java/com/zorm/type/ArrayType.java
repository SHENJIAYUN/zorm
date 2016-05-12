package com.zorm.type;

import java.io.Serializable;
import java.lang.reflect.Array;

import com.zorm.collection.PersistentCollection;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionImplementor;

public class ArrayType extends CollectionType {
	private final Class elementClass;
	private final Class arrayClass;
	
	public ArrayType(TypeFactory.TypeScope typeScope, String role, String propertyRef, Class elementClass) {
		super( typeScope, role, propertyRef );
		this.elementClass = elementClass;
		arrayClass = Array.newInstance(elementClass, 0).getClass();
	}
	
	public Class getReturnedClass() {
		return arrayClass;
	}
	
	public boolean isArrayType() {
		return true;
	}

	@Override
	public PersistentCollection wrap(SessionImplementor session,
			Object collection) {
		return null;
	}

	@Override
	public PersistentCollection instantiate(SessionImplementor session,
			CollectionPersister persister, Serializable key) {
		return null;
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
