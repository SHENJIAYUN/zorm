package com.zorm.event;

import com.zorm.collection.PersistentCollection;
import com.zorm.engine.PersistenceContext;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionImplementor;
import com.zorm.type.CollectionType;
import com.zorm.type.Type;

public class WrapVisitor extends ProxyVisitor {
	boolean substitute = false;

	boolean isSubstitutionRequired() {
		return substitute;
	}

	WrapVisitor(EventSource session) {
		super(session);
	}

	@Override
	void processValue(int i, Object[] values, Type[] types) {
		Object result = processValue(values[i], types[i]);
		if (result != null) {
			substitute = true;
			values[i] = result;
		}
	}

	@Override
	Object processCollection(Object collection, CollectionType collectionType) {

		if (collection != null && (collection instanceof PersistentCollection)) {

			final SessionImplementor session = getSession();
			PersistentCollection coll = (PersistentCollection) collection;
			if (coll.setCurrentSession(session)) {
				// reattachCollection( coll, collectionType );
			}
			return null;

		} else {
			return processArrayOrNewCollection(collection, collectionType);
		}
	}

	final Object processArrayOrNewCollection(Object collection,
			CollectionType collectionType) {

		final SessionImplementor session = getSession();

		if (collection == null) {
			return null;
		} else {
			CollectionPersister persister = session.getFactory().getCollectionPersister(collectionType.getRole());

			final PersistenceContext persistenceContext = session.getPersistenceContext();
			if (collectionType.hasHolder()) {

				if (collection == CollectionType.UNFETCHED_COLLECTION)
					return null;

				PersistentCollection ah = persistenceContext
						.getCollectionHolder(collection);
				if (ah == null) {
					ah = collectionType.wrap(session, collection);
					persistenceContext.addNewCollection(persister, ah);
					persistenceContext.addCollectionHolder(ah);
				}
				return null;
			} else {

				PersistentCollection persistentCollection = collectionType.wrap(session, collection);
				persistenceContext.addNewCollection(persister, persistentCollection);

				return persistentCollection; // Force a substitution!

			}

		}

	}

}
