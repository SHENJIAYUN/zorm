package com.zorm.action;

import java.io.Serializable;

import com.zorm.collection.PersistentCollection;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionImplementor;

public class CollectionRemoveAction extends CollectionAction {
	private static final long serialVersionUID = 6879461112844962484L;
	private boolean emptySnapshot;
	private final Object affectedOwner;
	
	public CollectionRemoveAction(
			final PersistentCollection collection,
			final CollectionPersister persister,
			final Serializable id,
			final boolean emptySnapshot,
			final SessionImplementor session) {
	super( persister, collection, id, session );
	if (collection == null) {
		throw new AssertionFailure("collection == null");
	}
	this.emptySnapshot = emptySnapshot;
	this.affectedOwner = session.getPersistenceContext().getLoadedCollectionOwnerOrNull( collection );
   }
	
	public CollectionRemoveAction(
			final Object affectedOwner,
			final CollectionPersister persister,
			final Serializable id,
			final boolean emptySnapshot,
			final SessionImplementor session) {
	super( persister, null, id, session );
	if (affectedOwner == null) {
		throw new AssertionFailure("affectedOwner == null");
	}
	this.emptySnapshot = emptySnapshot;
	this.affectedOwner = affectedOwner;
   }

	@Override
	public void execute() throws ZormException {
		
		final PersistentCollection collection = getCollection();
		if (collection!=null) {
			getSession().getPersistenceContext()
				.getCollectionEntry(collection)
				.afterAction(collection);
		}
	}
}
