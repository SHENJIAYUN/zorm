package com.zorm.action;

import java.io.Serializable;

import com.zorm.collection.PersistentCollection;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionImplementor;

public final class CollectionRecreateAction extends CollectionAction {
	
	private static final long serialVersionUID = 6455100534114072302L;

	public CollectionRecreateAction(
			final PersistentCollection collection,
			final CollectionPersister persister,
			final Serializable id,
			final SessionImplementor session) {
		super( persister, collection, id, session );
	}


	@Override
	public void execute() throws ZormException {
		final PersistentCollection collection = getCollection();
		
		getPersister().recreate( collection, getKey(), getSession() );
		
		getSession().getPersistenceContext().getCollectionEntry(collection).afterAction(collection);
	}
}
