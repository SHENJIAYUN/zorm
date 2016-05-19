package com.zorm.type;

import java.io.Serializable;
import java.util.HashSet;

import com.zorm.collection.PersistentCollection;
import com.zorm.collection.PersistentSet;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionImplementor;

public class SetType extends CollectionType{

	private static final long serialVersionUID = -8533365144553250962L;

	public SetType(TypeFactory.TypeScope typeScope, String role, String propertyRef) {
		super( typeScope, role, propertyRef );
	}
	
	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister, Serializable key) {
		return new PersistentSet(session);
	}

	public Class getReturnedClass() {
		return java.util.Set.class;
	}
	
	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		return new PersistentSet( session, (java.util.Set) collection );
	}
	
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0
		       ? new HashSet()
		       : new HashSet( anticipatedSize + (int)( anticipatedSize * .75f ), .75f );
	}
}
