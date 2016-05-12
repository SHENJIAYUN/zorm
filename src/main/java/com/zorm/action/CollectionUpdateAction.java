package com.zorm.action;

import java.io.Serializable;

import com.zorm.collection.PersistentCollection;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionImplementor;

public class CollectionUpdateAction extends CollectionAction{
	
	private static final long serialVersionUID = -1852685923587885530L;
	private final boolean emptySnapshot;

	public CollectionUpdateAction(
				final PersistentCollection collection,
				final CollectionPersister persister,
				final Serializable id,
				final boolean emptySnapshot,
				final SessionImplementor session) {
		super( persister, collection, id, session );
		this.emptySnapshot = emptySnapshot;
	}

	@Override
	public void execute() throws ZormException {
		
	}
}
