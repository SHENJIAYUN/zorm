package com.zorm.type;

import java.io.Serializable;
import java.util.Comparator;

import com.zorm.collection.PersistentCollection;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionImplementor;

public class SortedSetType extends SetType {

	private final Comparator comparator;
	
	public SortedSetType(TypeFactory.TypeScope typeScope, String role, String propertyRef, Comparator comparator) {
		super( typeScope, role, propertyRef );
		this.comparator = comparator;
	}
	
	@Override
	public PersistentCollection instantiate(SessionImplementor session,
			CollectionPersister persister, Serializable key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PersistentCollection wrap(SessionImplementor session,
			Object collection) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		// TODO Auto-generated method stub
		return null;
	}

}
