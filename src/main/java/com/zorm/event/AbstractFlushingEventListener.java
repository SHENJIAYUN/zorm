package com.zorm.event;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.action.CascadingAction;
import com.zorm.action.CollectionRecreateAction;
import com.zorm.action.CollectionRemoveAction;
import com.zorm.action.CollectionUpdateAction;
import com.zorm.collection.PersistentCollection;
import com.zorm.engine.ActionQueue;
import com.zorm.engine.Cascade;
import com.zorm.engine.CollectionEntry;
import com.zorm.engine.CollectionKey;
import com.zorm.engine.Collections;
import com.zorm.engine.PersistenceContext;
import com.zorm.engine.Status;
import com.zorm.entity.EntityEntry;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.session.SessionImplementor;
import com.zorm.util.IdentityMap;

public abstract class AbstractFlushingEventListener implements Serializable{

	private static final long serialVersionUID = 8053922534625842758L;
	private static final Log log = LogFactory.getLog(AbstractFlushingEventListener.class);
	
	protected void performExecutions(EventSource session) {
		try {
			session.getTransactionCoordinator().getJdbcCoordinator().flushBeginning();
			session.getPersistenceContext().setFlushing( true );
			session.getActionQueue().prepareActions();
			session.getActionQueue().executeActions();
		}
		finally {
			session.getPersistenceContext().setFlushing( false );
			session.getTransactionCoordinator().getJdbcCoordinator().flushEnding();
		}
	}
	
	protected void flushEverythingToExecutions(FlushEvent event) throws ZormException{
		log.debug("Flushing session");
		EventSource session = event.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		prepareEntityFlushes( session, persistenceContext );
		prepareCollectionFlushes(persistenceContext);
		persistenceContext.setFlushing(true);
		try{
			flushEntities( event, persistenceContext );
			flushCollections(session,persistenceContext);
		}
		finally{
			persistenceContext.setFlushing(false);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void postFlush(SessionImplementor session) {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		persistenceContext.getCollectionsByKey().clear();
		persistenceContext.getBatchFetchQueue().clearSubselects(); 
		
		for ( Map.Entry<PersistentCollection, CollectionEntry> me : IdentityMap.concurrentEntries( persistenceContext.getCollectionEntries() ) ) {
			CollectionEntry collectionEntry = me.getValue();
			PersistentCollection persistentCollection = me.getKey();
			collectionEntry.postFlush(persistentCollection);
			if(collectionEntry.getLoadedPersister()==null){
				persistenceContext.getCollectionEntries()
				     .remove(persistentCollection);
			}
			else{
				CollectionKey collectionKey = new CollectionKey(
						collectionEntry.getLoadedPersister(), 
						collectionEntry.getLoadedKey());
				persistenceContext.getCollectionsByKey().put(collectionKey, persistentCollection);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void prepareCollectionFlushes(PersistenceContext persistenceContext) throws ZormException {

		log.info( "Dirty checking collections" );

		for ( Map.Entry<PersistentCollection,CollectionEntry> entry :
				IdentityMap.concurrentEntries( (Map<PersistentCollection,CollectionEntry>) persistenceContext.getCollectionEntries() )) {
			entry.getValue().preFlush( entry.getKey() );
		}
	}
	
	@SuppressWarnings("unchecked")
	private void flushCollections(final EventSource session, final PersistenceContext persistenceContext) throws ZormException {

		log.info( "Processing unreferenced collections" );

		for ( Map.Entry<PersistentCollection,CollectionEntry> me :
				IdentityMap.concurrentEntries( (Map<PersistentCollection,CollectionEntry>) persistenceContext.getCollectionEntries() )) {
			CollectionEntry ce = me.getValue();
			if ( !ce.isReached() && !ce.isIgnore() ) {
				Collections.processUnreachableCollection( me.getKey(), session );
			}
		}

		ActionQueue actionQueue = session.getActionQueue();
		for ( Map.Entry<PersistentCollection,CollectionEntry> me :
			IdentityMap.concurrentEntries( (Map<PersistentCollection,CollectionEntry>) persistenceContext.getCollectionEntries() )) {
			PersistentCollection coll = me.getKey();
			CollectionEntry ce = me.getValue();

			if ( ce.isDorecreate() ) {
				session.getInterceptor().onCollectionRecreate( coll, ce.getCurrentKey() );
				actionQueue.addAction(
						new CollectionRecreateAction(
								coll,
								ce.getCurrentPersister(),
								ce.getCurrentKey(),
								session
							)
					);
			}
			if ( ce.isDoremove() ) {
				session.getInterceptor().onCollectionRemove( coll, ce.getLoadedKey() );
				actionQueue.addAction(
						new CollectionRemoveAction(
								coll,
								ce.getLoadedPersister(),
								ce.getLoadedKey(),
								ce.isSnapshotEmpty(coll),
								session
							)
					);
			}
			if ( ce.isDoupdate() ) {
				session.getInterceptor().onCollectionUpdate( coll, ce.getLoadedKey() );
				actionQueue.addAction(
						new CollectionUpdateAction(
								coll,
								ce.getLoadedPersister(),
								ce.getLoadedKey(),
								ce.isSnapshotEmpty(coll),
								session
							)
					);
			}

		}

		actionQueue.sortCollectionActions();

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void flushEntities(final FlushEvent event,
			final PersistenceContext persistenceContext) {
		final EventSource source = event.getSession();
		final Iterable<FlushEntityEventListener> flushListeners = source
				.getFactory()
				.getServiceRegistry()
				.getService(EventListenerRegistry.class)
				.getEventListenerGroup(EventType.FLUSH_ENTITY)
				.listeners();
		
		for(Map.Entry me : IdentityMap.concurrentEntries(persistenceContext.getEntityEntries())){
			EntityEntry entry = (EntityEntry)me.getValue();
			Status status = entry.getStatus();
			
			if(status != Status.LOADING && status != Status.GONE){
				final FlushEntityEvent entityEvent = new FlushEntityEvent( source, me.getKey(), entry );
				for ( FlushEntityEventListener listener : flushListeners ) {
					listener.onFlushEntity( entityEvent );
				}
			}
		}
		source.getActionQueue().sortActions();
	}

	private void prepareEntityFlushes(EventSource session,PersistenceContext persistenceContext) {
		log.debug( "Processing flush-time cascades" );
		final Object anything = getAnything();
		for ( Map.Entry me : IdentityMap.concurrentEntries( persistenceContext.getEntityEntries() ) ) {
			EntityEntry entry = (EntityEntry) me.getValue();
			Status status = entry.getStatus();
			if ( status == Status.MANAGED || status == Status.SAVING || status == Status.READ_ONLY ) {
				cascadeOnFlush( session, entry.getPersister(), me.getKey(), anything );
			}
		}
	}

	private void cascadeOnFlush(EventSource session, EntityPersister persister, Object object, Object anything) throws ZormException {
		session.getPersistenceContext().incrementCascadeLevel();
		try {
			new Cascade( getCascadingAction(), Cascade.BEFORE_FLUSH, session )
					.cascade( persister, object, anything );
		}
		finally {
			session.getPersistenceContext().decrementCascadeLevel();
		}
	}
	
	protected Object getAnything() {
		return null;
	}
	
	protected CascadingAction getCascadingAction() {
		return CascadingAction.SAVE_UPDATE;
	}
}
