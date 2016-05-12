package com.zorm.engine;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;

import com.zorm.LockMode;
import com.zorm.collection.PersistentCollection;
import com.zorm.entity.EntityEntry;
import com.zorm.entity.EntityKey;
import com.zorm.entity.EntityUniqueKey;
import com.zorm.exception.MappingException;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.session.SessionImplementor;
import com.zorm.util.IdentitySet;

public interface PersistenceContext {

	public NaturalIdHelper getNaturalIdHelper();

	public EntityEntry getEntry(Object entity);
	public static interface NaturalIdHelper {
		public static final Serializable INVALID_NATURAL_ID_REFERENCE = new Serializable() {};

		public Object[] removeLocalNaturalIdCrossReference(
				EntityPersister persister, Serializable id, Object[] state);

		public Object[] extractNaturalIdValues(Object[] state,
				EntityPersister persister);

		public void manageLocalNaturalIdCrossReference(EntityPersister persister,
				Serializable id, Object[] state, Object[] previousState,
				CachedNaturalIdValueSource source);

	}
	public Object unproxyAndReassociate(Object  maybeProxy) throws ZormException;

	public Object getEntity(EntityKey key);

	public EntityEntry addEntity(
			final Object entity,
			final Status status,
			final Object[] loadedState,
			final EntityKey entityKey,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final boolean disableVersionIncrement,
			boolean lazyPropertiesAreUnfetched);

	/**
	 * Generates an appropriate EntityEntry instance and adds it 
	 * to the event source's internal caches.
	 */
	public EntityEntry addEntry(
			final Object entity,
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Serializable id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final boolean disableVersionIncrement,
			boolean lazyPropertiesAreUnfetched);

	public void reassociateProxy(Object value, Serializable id);

	public Map getEntityEntries();

	public void setFlushing(boolean flushing);

	public void registerInsertedKey(EntityPersister persister, Serializable id);

	public void afterTransactionCompletion();

	public void setEntryStatus(EntityEntry entityEntry, Status status);

	public HashSet getNullifiableEntityKeys();

	public void checkUniqueness(EntityKey key, Object entity);

	public EntityEntry removeEntry(Object entity);

	public Object removeEntity(EntityKey key);

	public boolean isDefaultReadOnly();

	public void setDefaultReadOnly(boolean readOnly);

	public void beforeLoad();

	public void afterLoad();

	public Serializable getOwnerId(String entityName, String propertyName,
			Object target, Map mergeMap);

	public Object getProxy(EntityKey key);

	public SessionImplementor getSession();

	public int incrementCascadeLevel();

	public void addChildParent(Object child, Object parent);

	public void removeChildParent(Object child);

	public int decrementCascadeLevel();

	public CollectionEntry getCollectionEntry(PersistentCollection coll);

	public Map getCollectionEntries();

	Object getLoadedCollectionOwnerOrNull(PersistentCollection collection);

	public Object getCollectionOwner(Serializable key,
			CollectionPersister collectionPersister) throws MappingException;

	public Object getEntity(EntityUniqueKey euk);

	public Map getCollectionsByKey();

	public BatchFetchQueue getBatchFetchQueue();

	public Object[] getDatabaseSnapshot(Serializable id,EntityPersister persister);

	public PersistentCollection getCollectionHolder(Object array);

	public boolean containsCollection(PersistentCollection collection);

	public void addNewCollection(CollectionPersister persister,PersistentCollection collection);

	public void addCollectionHolder(PersistentCollection holder);

	public boolean isEntryFor(Object entity);

	public LoadContexts getLoadContexts();

	public PersistentCollection useUnownedCollection(CollectionKey collectionKey);

	public void addUninitializedCollection(CollectionPersister persister,PersistentCollection collection, Serializable key);

	public void addNonLazyCollection(PersistentCollection collection);

	public PersistentCollection getCollection(CollectionKey collectionKey);

	public Serializable getSnapshot(PersistentCollection coll);

	public void clear();

	public Object proxyFor(Object result) throws ZormException;

	public void addUnownedCollection(CollectionKey key, PersistentCollection collection);

	public CollectionEntry addInitializedCollection(CollectionPersister persister, PersistentCollection collection,Serializable key);

}
