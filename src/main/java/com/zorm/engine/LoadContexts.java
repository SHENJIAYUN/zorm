package com.zorm.engine;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.zorm.collection.PersistentCollection;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionImplementor;

public class LoadContexts {
	private final PersistenceContext persistenceContext;
	private Map<ResultSet,CollectionLoadContext> collectionLoadContexts;
	private Map<ResultSet,EntityLoadContext> entityLoadContexts;

	private Map<CollectionKey,LoadingCollectionEntry> xrefLoadingCollectionEntries;

	/**
	 * Creates and binds this to the given persistence context.
	 *
	 * @param persistenceContext The persistence context to which this
	 * will be bound.
	 */
	public LoadContexts(PersistenceContext persistenceContext) {
		this.persistenceContext = persistenceContext;
	}

	/**
	 * Retrieves the persistence context to which this is bound.
	 *
	 * @return The persistence context to which this is bound.
	 */
	public PersistenceContext getPersistenceContext() {
		return persistenceContext;
	}

	private SessionImplementor getSession() {
		return getPersistenceContext().getSession();
	}


	// cleanup code ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 	/**
	 * Release internal state associated with the given result set.
	 * <p/>
	 * This should be called when we are done with processing said result set,
	 * ideally as the result set is being closed.
	 *
	 * @param resultSet The result set for which it is ok to release
	 * associated resources.
	 */
	public void cleanup(ResultSet resultSet) {
		if ( collectionLoadContexts != null ) {
			CollectionLoadContext collectionLoadContext = collectionLoadContexts.remove( resultSet );
			collectionLoadContext.cleanup();
		}
		if ( entityLoadContexts != null ) {
			EntityLoadContext entityLoadContext = entityLoadContexts.remove( resultSet );
			entityLoadContext.cleanup();
		}
	}

	/**
	 * Release internal state associated with *all* result sets.
	 * <p/>
	 * This is intended as a "failsafe" process to make sure we get everything
	 * cleaned up and released.
	 */
	public void cleanup() {
		if ( collectionLoadContexts != null ) {
			for ( CollectionLoadContext collectionLoadContext : collectionLoadContexts.values() ) {
				collectionLoadContext.cleanup();
			}
			collectionLoadContexts.clear();
		}
		if ( entityLoadContexts != null ) {
			for ( EntityLoadContext entityLoadContext : entityLoadContexts.values() ) {
				entityLoadContext.cleanup();
			}
			entityLoadContexts.clear();
		}
	}


	// Collection load contexts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Do we currently have any internal entries corresponding to loading
	 * collections?
	 *
	 * @return True if we currently hold state pertaining to loading collections;
	 * false otherwise.
	 */
	public boolean hasLoadingCollectionEntries() {
		return ( collectionLoadContexts != null && !collectionLoadContexts.isEmpty() );
	}

	/**
	 * Do we currently have any registered internal entries corresponding to loading
	 * collections?
	 *
	 * @return True if we currently hold state pertaining to a registered loading collections;
	 * false otherwise.
	 */
	public boolean hasRegisteredLoadingCollectionEntries() {
		return ( xrefLoadingCollectionEntries != null && !xrefLoadingCollectionEntries.isEmpty() );
	}


	/**
	 * Get the {@link CollectionLoadContext} associated with the given
	 * {@link ResultSet}, creating one if needed.
	 *
	 * @param resultSet The result set for which to retrieve the context.
	 * @return The processing context.
	 */
	public CollectionLoadContext getCollectionLoadContext(ResultSet resultSet) {
		CollectionLoadContext context = null;
		if ( collectionLoadContexts == null ) {
			collectionLoadContexts = new IdentityHashMap<ResultSet, CollectionLoadContext>( 8 );
		}
		else {
			context = collectionLoadContexts.get(resultSet);
		}
		if ( context == null ) {
			context = new CollectionLoadContext( this, resultSet );
			collectionLoadContexts.put( resultSet, context );
		}
		return context;
	}

	/**
	 * Attempt to locate the loading collection given the owner's key.  The lookup here
	 * occurs against all result-set contexts...
	 *
	 * @param persister The collection persister
	 * @param ownerKey The owner key
	 * @return The loading collection, or null if not found.
	 */
	public PersistentCollection locateLoadingCollection(CollectionPersister persister, Serializable ownerKey) {
		LoadingCollectionEntry lce = locateLoadingCollectionEntry( new CollectionKey( persister, ownerKey ) );
		if ( lce != null ) {
			return lce.getCollection();
		}
		return null;
	}

	void registerLoadingCollectionXRef(CollectionKey entryKey, LoadingCollectionEntry entry) {
		if ( xrefLoadingCollectionEntries == null ) {
			xrefLoadingCollectionEntries = new HashMap<CollectionKey,LoadingCollectionEntry>();
		}
		xrefLoadingCollectionEntries.put( entryKey, entry );
	}

	void unregisterLoadingCollectionXRef(CollectionKey key) {
		if ( !hasRegisteredLoadingCollectionEntries() ) {
			return;
		}
		xrefLoadingCollectionEntries.remove(key);
	 }

	@SuppressWarnings( {"UnusedDeclaration"})
	Map getLoadingCollectionXRefs() {
 		return xrefLoadingCollectionEntries;
 	}

	LoadingCollectionEntry locateLoadingCollectionEntry(CollectionKey key) {
		if ( xrefLoadingCollectionEntries == null ) {
			return null;
		}
		LoadingCollectionEntry rtn = xrefLoadingCollectionEntries.get( key );
		return rtn;
	}

	void cleanupCollectionXRefs(Set<CollectionKey> entryKeys) {
		for ( CollectionKey entryKey : entryKeys ) {
			xrefLoadingCollectionEntries.remove( entryKey );
		}
	}


	// Entity load contexts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// 	* currently, not yet used...

	@SuppressWarnings( {"UnusedDeclaration"})
	public EntityLoadContext getEntityLoadContext(ResultSet resultSet) {
		EntityLoadContext context = null;
		if ( entityLoadContexts == null ) {
			entityLoadContexts = new IdentityHashMap<ResultSet, EntityLoadContext>( 8 );
		}
		else {
			context = entityLoadContexts.get( resultSet );
		}
		if ( context == null ) {
			context = new EntityLoadContext( this, resultSet );
			entityLoadContexts.put( resultSet, context );
		}
		return context;
	}
}
