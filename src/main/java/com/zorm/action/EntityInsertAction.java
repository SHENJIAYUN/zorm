package com.zorm.action;

import java.io.Serializable;

import com.zorm.engine.Versioning;
import com.zorm.entity.EntityEntry;
import com.zorm.entity.EntityKey;
import com.zorm.event.EventListenerGroup;
import com.zorm.event.EventType;
import com.zorm.event.PreInsertEvent;
import com.zorm.event.PreInsertEventListener;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;

public final class EntityInsertAction extends AbstractEntityInsertAction {

	private Object version;
	private Object cacheEntry;

	public EntityInsertAction(
			Serializable id,
			Object[] state,
			Object instance,
			Object version,
			EntityPersister persister,
			boolean isVersionIncrementDisabled,
			SessionImplementor session) throws ZormException {
		super( id, state, instance, isVersionIncrementDisabled, persister, session );
		this.version = version;
	}

	@Override
	public boolean isEarlyInsert() {
		return false;
	}

	@Override
	protected EntityKey getEntityKey() {
		return getSession().generateEntityKey( getId(), getPersister() );
	}

	@Override
	public void execute() throws ZormException {
		nullifyTransientReferencesIfNotAlready();

		EntityPersister persister = getPersister();
		SessionImplementor session = getSession();
		Object instance = getInstance();
		Serializable id = getId();

		boolean veto = preInsert();

		if ( !veto ) {
			
			persister.insert( id, getState(), instance, session );
		
			EntityEntry entry = session.getPersistenceContext().getEntry( instance );
			if ( entry == null ) {
				throw new AssertionFailure( "possible non-threadsafe access to session" );
			}
			
			entry.postInsert( getState() );
	
			if ( persister.hasInsertGeneratedProperties() ) {
				persister.processInsertGeneratedProperties( id, instance, getState(), session );
				if ( persister.isVersionPropertyGenerated() ) {
					version = Versioning.getVersion( getState(), persister );
				}
				entry.postUpdate(instance, getState(), version);
			}

			getSession().getPersistenceContext().registerInsertedKey( getPersister(), getId() );
		}

		handleNaturalIdPostSaveNotifications();

		postInsert();

		markExecuted();
	}

	private void postInsert() {
//		EventListenerGroup<PostInsertEventListener> listenerGroup = listenerGroup( EventType.POST_INSERT );
//		if ( listenerGroup.isEmpty() ) {
//			return;
//		}
//		final PostInsertEvent event = new PostInsertEvent(
//				getInstance(),
//				getId(),
//				getState(),
//				getPersister(),
//				eventSource()
//		);
//		for ( PostInsertEventListener listener : listenerGroup.listeners() ) {
//			listener.onPostInsert( event );
//		}
	}

	private void postCommitInsert() {
//		EventListenerGroup<PostInsertEventListener> listenerGroup = listenerGroup( EventType.POST_COMMIT_INSERT );
//		if ( listenerGroup.isEmpty() ) {
//			return;
//		}
//		final PostInsertEvent event = new PostInsertEvent(
//				getInstance(),
//				getId(),
//				getState(),
//				getPersister(),
//				eventSource()
//		);
//		for ( PostInsertEventListener listener : listenerGroup.listeners() ) {
//			listener.onPostInsert( event );
//		}
	}

	private boolean preInsert() {
		boolean veto = false;

		EventListenerGroup<PreInsertEventListener> listenerGroup = listenerGroup( EventType.PRE_INSERT );
		if ( listenerGroup.isEmpty() ) {
			return veto;
		}
		final PreInsertEvent event = new PreInsertEvent( getInstance(), getId(), getState(), getPersister(), eventSource() );
		for ( PreInsertEventListener listener : listenerGroup.listeners() ) {
			veto |= listener.onPreInsert( event );
		}
		return veto;
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SessionImplementor session) throws ZormException {
//		EntityPersister persister = getPersister();
//		if ( success && isCachePutEnabled( persister, getSession() ) ) {
//			final CacheKey ck = getSession().generateCacheKey( getId(), persister.getIdentifierType(), persister.getRootEntityName() );
//			boolean put = persister.getCacheAccessStrategy().afterInsert( ck, cacheEntry, version );
//			
//			if ( put && getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
//				getSession().getFactory().getStatisticsImplementor()
//						.secondLevelCachePut( getPersister().getCacheAccessStrategy().getRegion().getName() );
//			}
//		}
//		postCommitInsert();
	}

	@Override
	protected boolean hasPostCommitEventListeners() {
		//return ! listenerGroup( EventType.POST_COMMIT_INSERT ).isEmpty();
		return false;
	}
	
	private boolean isCachePutEnabled(EntityPersister persister, SessionImplementor session) {
//		return persister.hasCache()
//				&& !persister.isCacheInvalidationRequired()
//				&& session.getCacheMode().isPutEnabled();
		return false;
	}

}
