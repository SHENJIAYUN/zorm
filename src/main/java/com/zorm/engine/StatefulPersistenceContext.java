package com.zorm.engine;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zorm.LockMode;
import com.zorm.collection.PersistentCollection;
import com.zorm.entity.EntityEntry;
import com.zorm.entity.EntityKey;
import com.zorm.entity.EntityUniqueKey;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.MappingException;
import com.zorm.exception.NonUniqueObjectException;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.session.SessionImplementor;
import com.zorm.type.CollectionType;
import com.zorm.util.IdentityMap;
import com.zorm.util.MarkerObject;

@SuppressWarnings("rawtypes")
public class StatefulPersistenceContext implements PersistenceContext{
	
	public static final Object NO_ROW = new MarkerObject( "NO_ROW" );
	private static final int INIT_COLL_SIZE = 8;
	private SessionImplementor session;
	private Map<EntityKey, Object> entitiesByKey;
	private Map<EntityUniqueKey, Object> entitiesByUniqueKey;
	private Map<Object,EntityEntry> entityEntries;
	private Map<EntityKey, Object> proxiesByKey;
	private Map<EntityKey, Object> entitySnapshotsByKey;

	private IdentityMap<PersistentCollection, CollectionEntry> collectionEntries;
	private HashSet<EntityKey> nullifiableEntityKeys;
	private Map<Object, PersistentCollection> arrayHolders;
	private Map<Object,Object> parentsByChild;
	private LoadContexts loadContexts;
	private List<PersistentCollection> nonlazyCollections;

	private int cascading = 0;
	private int loadCounter = 0;
	private boolean flushing = false;

	private boolean defaultReadOnly = false;
	private boolean hasNonReadOnlyEntities = false;
	private Map<CollectionKey, PersistentCollection> collectionsByKey;
	private Map<CollectionKey,PersistentCollection> unownedCollections;
	private BatchFetchQueue batchFetchQueue;

	
	public StatefulPersistenceContext(SessionImplementor session) {
		this.session = session;

		entitiesByKey = new HashMap<EntityKey, Object>( INIT_COLL_SIZE );
		entitiesByUniqueKey = new HashMap<EntityUniqueKey, Object>( INIT_COLL_SIZE );
		proxiesByKey = new ConcurrentReferenceHashMap<EntityKey, Object>( INIT_COLL_SIZE, .75f, 1, ConcurrentReferenceHashMap.ReferenceType.STRONG, ConcurrentReferenceHashMap.ReferenceType.WEAK, null );
		entitySnapshotsByKey = new HashMap<EntityKey, Object>( INIT_COLL_SIZE );
		collectionEntries = IdentityMap.instantiateSequenced( INIT_COLL_SIZE );
		entityEntries = IdentityMap.instantiateSequenced( INIT_COLL_SIZE );
		parentsByChild = IdentityMap.instantiateSequenced( INIT_COLL_SIZE );
		collectionsByKey = new HashMap<CollectionKey, PersistentCollection>( INIT_COLL_SIZE );
		nullifiableEntityKeys = new HashSet<EntityKey>();
		arrayHolders = new IdentityHashMap<Object, PersistentCollection>( INIT_COLL_SIZE );
		initTransientState();
	}


	@Override
	public void registerInsertedKey(EntityPersister persister, Serializable id) {
		// we only are worried about registering these if the persister defines caching
//		if ( persister.hasCache() ) {
//			if ( insertedKeysMap == null ) {
//				insertedKeysMap = new HashMap<String, List<Serializable>>();
//			}
//			final String rootEntityName = persister.getRootEntityName();
//			List<Serializable> insertedEntityIds = insertedKeysMap.get( rootEntityName );
//			if ( insertedEntityIds == null ) {
//				insertedEntityIds = new ArrayList<Serializable>();
//				insertedKeysMap.put( rootEntityName, insertedEntityIds );
//			}
//			insertedEntityIds.add( id );
//		}
	}
	
	@Override
	public Object unproxyAndReassociate(Object maybeProxy) throws ZormException {
		return maybeProxy;
	}

	@Override
	public EntityEntry getEntry(Object entity) {
		return entityEntries.get(entity);
	}


	public int getCascadeLevel() {
		return cascading;
	}


	@Override
	public Object getEntity(EntityKey key) {
		return entitiesByKey.get(key);
	}

	@Override
	public void reassociateProxy(Object value, Serializable id) throws MappingException {
		
	}

	@Override
	public EntityEntry addEntry(final Object entity,
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Serializable id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final boolean disableVersionIncrement,
			boolean lazyPropertiesAreUnfetched) {
		
		EntityEntry e = new EntityEntry(status, loadedState, rowId, id, version, lockMode, existsInDatabase, persister, persister.getEntityMode(), session.getTenantIdentifier(), disableVersionIncrement, lazyPropertiesAreUnfetched, this);
		//[实例对象：实体项]
		entityEntries.put(entity, e);
		setHasNonReadOnlyEnties(status);
		return e;
	}
	
	private void setHasNonReadOnlyEnties(Status status) {
		if ( status==Status.DELETED || status==Status.MANAGED || status==Status.SAVING ) {
			hasNonReadOnlyEntities = true;
		}
	}


	@Override
	public EntityEntry addEntity(Object entity, Status status,
			Object[] loadedState, EntityKey entityKey, Object version,
			LockMode lockMode, boolean existsInDatabase,
			EntityPersister persister, boolean disableVersionIncrement,
			boolean lazyPropertiesAreUnfetched) {
		addEntity( entityKey, entity );
		return addEntry(
				entity,
				status,
				loadedState,
				null,
				entityKey.getIdentifier(),
				version,
				lockMode,
				existsInDatabase,
				persister,
				disableVersionIncrement,
				lazyPropertiesAreUnfetched
		);
	}


	private void addEntity(EntityKey key, Object entity) {
		entitiesByKey.put(key, entity);
	}


	@Override
	public Map getEntityEntries() {
		return entityEntries;
	}


	@Override
	public void setFlushing(boolean flushing) {
		final boolean afterFlush = this.flushing && ! flushing;
		this.flushing = flushing;
		if(afterFlush){
		}
	}


	public void afterTransactionCompletion() {
		cleanUpInsertedKeysAfterTransaction();
		for ( EntityEntry o : entityEntries.values() ) {
			o.setLockMode( LockMode.NONE );
		}
	}

	private HashMap<String,List<Serializable>> insertedKeysMap;

	private void cleanUpInsertedKeysAfterTransaction() {
		if ( insertedKeysMap != null ) {
			insertedKeysMap.clear();
		}
		
	}


	@Override
	public void setEntryStatus(EntityEntry entityEntry, Status status) {
		entityEntry.setStatus(status);
		setHasNonReadOnlyEnties(status);
	}


	@Override
	public HashSet getNullifiableEntityKeys() {
		return nullifiableEntityKeys;
	}


	@Override
	public void checkUniqueness(EntityKey key, Object object) {
		Object entity = getEntity(key);
		if ( entity == object ) {
			throw new AssertionFailure( "object already associated, but no entry was found" );
		}
		if ( entity != null ) {
			throw new NonUniqueObjectException( key.getIdentifier(), key.getEntityName() );
		}
	}


	@Override
	public EntityEntry removeEntry(Object entity) {
       return entityEntries.remove(entity);
	}


	@Override
	public Object removeEntity(EntityKey key) {
		Object entity = entitiesByKey.remove(key);
		Iterator iter = entitiesByUniqueKey.values().iterator();
		while ( iter.hasNext() ) {
			if ( iter.next()==entity ) iter.remove();
		}
		// Clear all parent cache
		parentsByChild.clear();
		entitySnapshotsByKey.remove(key);
		nullifiableEntityKeys.remove(key);
		//getBatchFetchQueue().removeBatchLoadableEntityKey(key);
		//getBatchFetchQueue().removeSubselect(key);
		return entity;
	}
	
	private EntityPersister locateProperPersister(EntityPersister persister) {
		return session.getFactory().getEntityPersister( persister.getRootEntityName() );
	}
	
	private final NaturalIdHelper naturalIdHelper = new NaturalIdHelper() {
		@SuppressWarnings("unused")
		@Override
		public void manageLocalNaturalIdCrossReference(
				EntityPersister persister,
				Serializable id,
				Object[] state,
				Object[] previousState,
				CachedNaturalIdValueSource source) {
			if ( !persister.hasNaturalIdentifier() ) {
				// nothing to do
				return;
			}

			persister = locateProperPersister( persister );
			final Object[] naturalIdValues = extractNaturalIdValues( state, persister );

		}
		
		@Override
		public Object[] extractNaturalIdValues(Object[] state, EntityPersister persister) {
			final int[] naturalIdPropertyIndexes = persister.getNaturalIdentifierProperties();
			if ( state.length == naturalIdPropertyIndexes.length ) {
				return state;
			}

			final Object[] naturalIdValues = new Object[naturalIdPropertyIndexes.length];
			for ( int i = 0; i < naturalIdPropertyIndexes.length; i++ ) {
				naturalIdValues[i] = state[naturalIdPropertyIndexes[i]];
			}
			return naturalIdValues;
		}

		@Override
		public Object[] removeLocalNaturalIdCrossReference(
				EntityPersister persister, Serializable id, Object[] state) {
			return null;
		}
	};


	@Override
	public NaturalIdHelper getNaturalIdHelper() {
		return naturalIdHelper;
	}


	@Override
	public boolean isDefaultReadOnly() {
		return defaultReadOnly;
	}
	
	@Override
	public void setDefaultReadOnly(boolean readOnly) {
       this.defaultReadOnly = readOnly;
	}
	
	@Override
	public void beforeLoad() {
	  loadCounter++;
	}
	
	@Override
	public void afterLoad() {
      loadCounter--;
	}


	@Override
	public Serializable getOwnerId(String entityName, String propertyName,
			Object target, Map mergeMap) {
		return null;
	}


	@Override
	public Object getProxy(EntityKey key) {
		return null;
	}


	@Override
	public SessionImplementor getSession() {
		return session;
	}
	
	@Override
	public int incrementCascadeLevel() {
		return ++cascading;
	}
	
	@Override
	public void addChildParent(Object child, Object parent) {
		parentsByChild.put(child, parent);
	}
	
	@Override
	public void removeChildParent(Object child) {
	   parentsByChild.remove(child);
	}
	
	@Override
	public int decrementCascadeLevel() {
		return --cascading;
	}
	
	@Override
	public CollectionEntry getCollectionEntry(PersistentCollection coll) {
		return collectionEntries.get(coll);
	}
	
	
	@Override
	public Map getCollectionEntries() {
		return collectionEntries;
	}
	
	@Override
	public Object getLoadedCollectionOwnerOrNull(PersistentCollection collection) {
		CollectionEntry ce = getCollectionEntry( collection );
		if ( ce.getLoadedPersister() == null ) {
			return null; // early exit...
		}
		Object loadedOwner = null;
		Serializable entityId = getLoadedCollectionOwnerIdOrNull( ce );
		if ( entityId != null ) {
			loadedOwner = getCollectionOwner( entityId, ce.getLoadedPersister() );
		}
		return loadedOwner;
	}
	
	@Override
	public Object getCollectionOwner(Serializable key, CollectionPersister collectionPersister) throws MappingException {
		final EntityPersister ownerPersister = collectionPersister.getOwnerEntityPersister();
		if ( ownerPersister.getIdentifierType().getReturnedClass().isInstance( key ) ) {
			return getEntity( session.generateEntityKey( key, collectionPersister.getOwnerEntityPersister() ) );
		}
		if ( ownerPersister.isInstance( key ) ) {
			final Serializable owenerId = ownerPersister.getIdentifier( key, session );
			if ( owenerId == null ) {
				return null;
			}
			return getEntity( session.generateEntityKey( owenerId, ownerPersister ) );
		}

		final CollectionType collectionType = collectionPersister.getCollectionType();
		if ( collectionType.getLHSPropertyName() != null ) {
			Object owner = getEntity(
					new EntityUniqueKey(
							ownerPersister.getEntityName(),
							collectionType.getLHSPropertyName(),
							key,
							collectionPersister.getKeyType(),
							ownerPersister.getEntityMode(),
							session.getFactory()
					)
			);
			if ( owner != null ) {
				return owner;
			}

			final Serializable ownerId = ownerPersister.getIdByUniqueKey( key, collectionType.getLHSPropertyName(), session );
			return getEntity( session.generateEntityKey( ownerId, ownerPersister ) );
		}

		return getEntity( session.generateEntityKey( key, collectionPersister.getOwnerEntityPersister() ) );
	}
	
	@Override
	public Object getEntity(EntityUniqueKey euk) {
		return entitiesByUniqueKey.get(euk);
	}
	
	private Serializable getLoadedCollectionOwnerIdOrNull(CollectionEntry ce) {
		if ( ce == null || ce.getLoadedKey() == null || ce.getLoadedPersister() == null ) {
			return null;
		}
		return ce.getLoadedPersister().getCollectionType().getIdOfOwnerOrNull( ce.getLoadedKey(), session );
	}
	
	@Override
	public Map getCollectionsByKey() {
		return collectionsByKey;
	}
	
	@Override
	public PersistentCollection getCollection(CollectionKey collectionKey) {
		return collectionsByKey.get( collectionKey );
	}
	
	@Override
	public Serializable getSnapshot(PersistentCollection coll) {
		return getCollectionEntry(coll).getSnapshot();
	}
	
	@Override
	public BatchFetchQueue getBatchFetchQueue() {
		if (batchFetchQueue==null) {
			batchFetchQueue = new BatchFetchQueue(this);
		}
		return batchFetchQueue;
	}


	@Override
	public Object[] getDatabaseSnapshot(Serializable id,
			EntityPersister persister) {
		final EntityKey key = session.generateEntityKey( id, persister );
		Object cached = entitySnapshotsByKey.get(key);
		if (cached!=null) {
			return cached==NO_ROW ? null : (Object[]) cached;
		}
		else {
			Object[] snapshot = persister.getDatabaseSnapshot( id, session );
			entitySnapshotsByKey.put( key, snapshot==null ? NO_ROW : snapshot );
			return snapshot;
		}
	}
	
	@Override
	public PersistentCollection getCollectionHolder(Object array) {
		return arrayHolders.get(array);
	}
	
	@Override
	public boolean containsCollection(PersistentCollection collection) {
		return collectionEntries.containsKey(collection);
	}
	
	@Override
	public void addNewCollection(CollectionPersister persister, PersistentCollection collection) {
		addCollection(collection, persister);
	}
	
	private void addCollection(PersistentCollection collection, CollectionPersister persister) {
		CollectionEntry ce = new CollectionEntry( persister, collection );
		collectionEntries.put( collection, ce );
	}

	@Override
	public void addCollectionHolder(PersistentCollection holder) {
		arrayHolders.put( holder.getValue(), holder );
	}
	
	@Override
	public boolean isEntryFor(Object entity) {
		return entityEntries.containsKey(entity);
	}
	
	@Override
	public LoadContexts getLoadContexts() {
		if ( loadContexts == null ) {
			loadContexts = new LoadContexts( this );
		}
		return loadContexts;
	}
	
	@Override
	public PersistentCollection useUnownedCollection(CollectionKey key) {
		return ( unownedCollections == null ) ? null : unownedCollections.remove( key );
	}
	
	@Override
	public void addUninitializedCollection(CollectionPersister persister, PersistentCollection collection, Serializable id) {
		CollectionEntry ce = new CollectionEntry(collection, persister, id, flushing);
		addCollection(collection, ce, id);
		if ( persister.getBatchSize() > 1 ) {
//			getBatchFetchQueue().addBatchLoadableCollection( collection, ce );
		}
	}
	
	private void initTransientState() {
		nonlazyCollections = new ArrayList<PersistentCollection>( INIT_COLL_SIZE );
	}
	
	@Override
	public void addNonLazyCollection(PersistentCollection collection) {
		nonlazyCollections.add(collection);
	}
	
	private void addCollection(PersistentCollection coll, CollectionEntry entry, Serializable key) {
		collectionEntries.put( coll, entry );
		CollectionKey collectionKey = new CollectionKey( entry.getLoadedPersister(), key );
		PersistentCollection old = collectionsByKey.put( collectionKey, coll );
		if ( old != null ) {
			if ( old == coll ) {
				throw new AssertionFailure("bug adding collection twice");
			}
			old.unsetSession( session );
			collectionEntries.remove( old );
		}
	}

	@Override
	public void clear() {
    	arrayHolders.clear();
		entitiesByKey.clear();
		entitiesByUniqueKey.clear();
		entityEntries.clear();
		parentsByChild.clear();
		entitySnapshotsByKey.clear();
		collectionsByKey.clear();
		collectionEntries.clear();
		if ( unownedCollections != null ) {
			unownedCollections.clear();
		}
		proxiesByKey.clear();
		nullifiableEntityKeys.clear();
		// defaultReadOnly is unaffected by clear()
		hasNonReadOnlyEntities = false;
		if ( loadContexts != null ) {
			loadContexts.cleanup();
		}
	}
	
	public CollectionEntry addInitializedCollection(CollectionPersister persister, PersistentCollection collection, Serializable id){
		CollectionEntry ce = new CollectionEntry(collection, persister, id, flushing);
		ce.postInitialize(collection);
		addCollection(collection, ce, id);
		return ce;
	}
	
	public void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeBoolean( defaultReadOnly );
		oos.writeBoolean( hasNonReadOnlyEntities );

		oos.writeInt( entitiesByKey.size() );
		Iterator itr = entitiesByKey.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			( ( EntityKey ) entry.getKey() ).serialize( oos );
			oos.writeObject( entry.getValue() );
		}

		oos.writeInt( entitiesByUniqueKey.size() );
		itr = entitiesByUniqueKey.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			( ( EntityUniqueKey ) entry.getKey() ).serialize( oos );
			oos.writeObject( entry.getValue() );
		}

		oos.writeInt( proxiesByKey.size() );
		itr = proxiesByKey.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			( (EntityKey) entry.getKey() ).serialize( oos );
			oos.writeObject( entry.getValue() );
		}

		oos.writeInt( entitySnapshotsByKey.size() );
		itr = entitySnapshotsByKey.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			( ( EntityKey ) entry.getKey() ).serialize( oos );
			oos.writeObject( entry.getValue() );
		}

		oos.writeInt( entityEntries.size() );
		itr = entityEntries.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			oos.writeObject( entry.getKey() );
			( ( EntityEntry ) entry.getValue() ).serialize( oos );
		}

		oos.writeInt( collectionsByKey.size() );
		itr = collectionsByKey.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			( ( CollectionKey ) entry.getKey() ).serialize( oos );
			oos.writeObject( entry.getValue() );
		}

		oos.writeInt( collectionEntries.size() );
		itr = collectionEntries.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			oos.writeObject( entry.getKey() );
			( ( CollectionEntry ) entry.getValue() ).serialize( oos );
		}

		oos.writeInt( arrayHolders.size() );
		itr = arrayHolders.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			oos.writeObject( entry.getKey() );
			oos.writeObject( entry.getValue() );
		}

		oos.writeInt( nullifiableEntityKeys.size() );
		for ( EntityKey entry : nullifiableEntityKeys ) {
			entry.serialize( oos );
		}
	}

	
	public static StatefulPersistenceContext deserialize(
			ObjectInputStream ois,
			SessionImplementor session) throws IOException, ClassNotFoundException {
		StatefulPersistenceContext rtn = new StatefulPersistenceContext( session );

		try {
			rtn.defaultReadOnly = ois.readBoolean();
			rtn.hasNonReadOnlyEntities = ois.readBoolean();

			int count = ois.readInt();
			rtn.entitiesByKey = new HashMap<EntityKey,Object>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.entitiesByKey.put( EntityKey.deserialize( ois, session ), ois.readObject() );
			}

			count = ois.readInt();
			rtn.entitiesByUniqueKey = new HashMap<EntityUniqueKey,Object>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.entitiesByUniqueKey.put( EntityUniqueKey.deserialize( ois, session ), ois.readObject() );
			}

			count = ois.readInt();
			rtn.proxiesByKey = new ConcurrentReferenceHashMap<EntityKey, Object>(
					count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count,
					.75f,
					1,
					ConcurrentReferenceHashMap.ReferenceType.STRONG,
					ConcurrentReferenceHashMap.ReferenceType.WEAK,
					null
			);
			for ( int i = 0; i < count; i++ ) {
				EntityKey ek = EntityKey.deserialize( ois, session );
				Object proxy = ois.readObject();
			}

			count = ois.readInt();
			rtn.entitySnapshotsByKey = new HashMap<EntityKey,Object>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.entitySnapshotsByKey.put( EntityKey.deserialize( ois, session ), ois.readObject() );
			}

			count = ois.readInt();
			rtn.entityEntries = IdentityMap.instantiateSequenced( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				Object entity = ois.readObject();
				EntityEntry entry = EntityEntry.deserialize( ois, rtn );
				rtn.entityEntries.put( entity, entry );
			}

			count = ois.readInt();
			rtn.collectionsByKey = new HashMap<CollectionKey,PersistentCollection>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.collectionsByKey.put( CollectionKey.deserialize( ois, session ), (PersistentCollection) ois.readObject() );
			}

			count = ois.readInt();
			rtn.collectionEntries = IdentityMap.instantiateSequenced( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				final PersistentCollection pc = ( PersistentCollection ) ois.readObject();
				final CollectionEntry ce = CollectionEntry.deserialize( ois, session );
				pc.setCurrentSession( session );
				rtn.collectionEntries.put( pc, ce );
			}

			count = ois.readInt();
			rtn.arrayHolders = new IdentityHashMap<Object, PersistentCollection>( count < INIT_COLL_SIZE ? INIT_COLL_SIZE : count );
			for ( int i = 0; i < count; i++ ) {
				rtn.arrayHolders.put( ois.readObject(), (PersistentCollection) ois.readObject() );
			}

			count = ois.readInt();
			rtn.nullifiableEntityKeys = new HashSet<EntityKey>();
			for ( int i = 0; i < count; i++ ) {
				rtn.nullifiableEntityKeys.add( EntityKey.deserialize( ois, session ) );
			}

		}
		catch ( ZormException he ) {
			throw new InvalidObjectException( he.getMessage() );
		}

		return rtn;
	}


	@Override
	public Object proxyFor(Object result) throws ZormException {
		return null;
	}
	
	@Override
	public void addUnownedCollection(CollectionKey key, PersistentCollection collection) {
		if (unownedCollections==null) {
			unownedCollections = new HashMap<CollectionKey,PersistentCollection>(INIT_COLL_SIZE);
		}
		unownedCollections.put( key, collection );
	}
}
