package com.zorm.engine;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.zorm.collection.PersistentCollection;
import com.zorm.entity.EntityMode;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.util.MessageHelper;

public class CollectionLoadContext {
	private final LoadContexts loadContexts;
	private final ResultSet resultSet;
	private Set localLoadingCollectionKeys = new HashSet();

	public CollectionLoadContext(LoadContexts loadContexts, ResultSet resultSet) {
		this.loadContexts = loadContexts;
		this.resultSet = resultSet;
	}

	public ResultSet getResultSet() {
		return resultSet;
	}

	public LoadContexts getLoadContext() {
		return loadContexts;
	}
	
    public void cleanup() {
		
	}

	@Override
    public String toString() {
		return super.toString() + "<rs=" + resultSet + ">";
	}

	@SuppressWarnings("unchecked")
	public PersistentCollection getLoadingCollection(final CollectionPersister persister, final Serializable key) {
		final EntityMode em = persister.getOwnerEntityPersister().getEntityMetamodel().getEntityMode();
		final CollectionKey collectionKey = new CollectionKey( persister, key, em );
		final LoadingCollectionEntry loadingCollectionEntry = loadContexts.locateLoadingCollectionEntry( collectionKey );
		if ( loadingCollectionEntry == null ) {
			PersistentCollection collection = loadContexts.getPersistenceContext().getCollection( collectionKey );
			if ( collection != null ) {
				if ( collection.wasInitialized() ) {
					return null;
				}
			}
			else {
				Object owner = loadContexts.getPersistenceContext().getCollectionOwner( key, persister );
				final boolean newlySavedEntity = owner != null
						&& loadContexts.getPersistenceContext().getEntry( owner ).getStatus() != Status.LOADING;
				if ( newlySavedEntity ) {
					return null;
				}
				collection = persister.getCollectionType().instantiate(
						loadContexts.getPersistenceContext().getSession(), persister, key );
			}
			collection.beforeInitialize( persister, -1 );
			collection.beginRead();
			localLoadingCollectionKeys.add( collectionKey );
			loadContexts.registerLoadingCollectionXRef( collectionKey, new LoadingCollectionEntry( resultSet, persister, key, collection ) );
			return collection;
		}
		if ( loadingCollectionEntry.getResultSet() == resultSet ) {
			return loadingCollectionEntry.getCollection();
		}
		return null;
	}

	public void endLoadingCollections(CollectionPersister persister) {
		SessionImplementor session = getLoadContext().getPersistenceContext().getSession();
		if ( !loadContexts.hasLoadingCollectionEntries()
				&& localLoadingCollectionKeys.isEmpty() ) {
			return;
		}
		List matches = null;
		Iterator iter = localLoadingCollectionKeys.iterator();
		while ( iter.hasNext() ) {
			final CollectionKey collectionKey = (CollectionKey) iter.next();
			final LoadingCollectionEntry lce = loadContexts.locateLoadingCollectionEntry( collectionKey );
			if ( lce == null ) {
			}
			else if ( lce.getResultSet() == resultSet && lce.getPersister() == persister ) {
				if ( matches == null ) {
					matches = new ArrayList();
				}
				matches.add( lce );
				if ( lce.getCollection().getOwner() == null ) {
					session.getPersistenceContext().addUnownedCollection(
							new CollectionKey(
									persister,
									lce.getKey(),
									persister.getOwnerEntityPersister().getEntityMetamodel().getEntityMode()
							),
							lce.getCollection()
					);
				}
				loadContexts.unregisterLoadingCollectionXRef( collectionKey );
				iter.remove();
			}
		}

		endLoadingCollections( persister, matches );
		if ( localLoadingCollectionKeys.isEmpty() ) {
			loadContexts.cleanup( resultSet );
		}
	}
	
	private void endLoadingCollections(CollectionPersister persister, List matchedCollectionEntries) {
		if ( matchedCollectionEntries == null ) {
			return;
		}

		final int count = matchedCollectionEntries.size();
		for ( int i = 0; i < count; i++ ) {
			LoadingCollectionEntry lce = ( LoadingCollectionEntry ) matchedCollectionEntries.get( i );
			endLoadingCollection( lce, persister );
		}

	}
	
	private void endLoadingCollection(LoadingCollectionEntry lce, CollectionPersister persister) {
		final SessionImplementor session = getLoadContext().getPersistenceContext().getSession();

		boolean hasNoQueuedAdds = lce.getCollection().endRead(); 

		if ( persister.getCollectionType().hasHolder() ) {
			getLoadContext().getPersistenceContext().addCollectionHolder( lce.getCollection() );
		}

		CollectionEntry ce = getLoadContext().getPersistenceContext().getCollectionEntry( lce.getCollection() );
		if ( ce == null ) {
			ce = getLoadContext().getPersistenceContext().addInitializedCollection( persister, lce.getCollection(), lce.getKey() );
		}
		else {
			ce.postInitialize( lce.getCollection() );
		}
	}

	
}
