package com.zorm.type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import com.zorm.collection.PersistentBag;
import com.zorm.collection.PersistentCollection;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionImplementor;

public class BagType extends CollectionType {
	private static final long serialVersionUID = -1542788042317140925L;

	public BagType(TypeFactory.TypeScope typeScope, String role, String propertyRef) {
		super( typeScope, role, propertyRef );
	}

	@SuppressWarnings("rawtypes")
	public Class getReturnedClass() {
		return java.util.Collection.class;
	}


	@SuppressWarnings("rawtypes")
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0 ? new ArrayList() : new ArrayList( anticipatedSize + 1 );
	}

	@Override
	public PersistentCollection wrap(SessionImplementor session,
			Object collection) {
		return new PersistentBag( session, (Collection) collection );
	}
	
	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister, Serializable key) {
		return new PersistentBag(session);
	}
}
