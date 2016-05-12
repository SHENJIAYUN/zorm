package com.zorm.type;

import java.io.Serializable;

import com.zorm.collection.PersistentCollection;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionImplementor;

public class ListType extends CollectionType {
	
	public ListType(TypeFactory.TypeScope typeScope, String role, String propertyRef) {
		super( typeScope, role, propertyRef );
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
