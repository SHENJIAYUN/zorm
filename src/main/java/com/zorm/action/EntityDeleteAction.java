package com.zorm.action;

import java.io.Serializable;

import com.zorm.engine.PersistenceContext;
import com.zorm.entity.EntityEntry;
import com.zorm.event.EventListenerGroup;
import com.zorm.event.EventType;
import com.zorm.event.PostDeleteEvent;
import com.zorm.event.PostDeleteEventListener;
import com.zorm.event.PreDeleteEventListener;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.session.SessionImplementor;

public final class EntityDeleteAction extends EntityAction {
	private final Object version;
	private final boolean isCascadeDeleteEnabled;
	private final Object[] state;

	private Object[] naturalIdValues;

	public EntityDeleteAction(
			final Serializable id,
	        final Object[] state,
	        final Object version,
	        final Object instance,
	        final EntityPersister persister,
	        final boolean isCascadeDeleteEnabled,
	        final SessionImplementor session) {
		super( session, id, instance, persister );
		this.version = version;
		this.isCascadeDeleteEnabled = isCascadeDeleteEnabled;
		this.state = state;

		// before remove we need to remove the local (transactional) natural id cross-reference
//		naturalIdValues = session.getPersistenceContext().getNaturalIdHelper().removeLocalNaturalIdCrossReference(
//				getPersister(),
//				getId(),
//				state
//		);
	}

	@Override
	public void execute() throws ZormException {
		Serializable id = getId();
		EntityPersister persister = getPersister();
		SessionImplementor session = getSession();
		Object instance = getInstance();

		boolean veto = preDelete();

		Object version = this.version;
		if ( persister.isVersionPropertyGenerated() ) {
			// we need to grab the version value from the entity, otherwise
			// we have issues with generated-version entities that may have
			// multiple actions queued during the same flush
			version = persister.getVersion( instance );
		}

		if ( !isCascadeDeleteEnabled && !veto ) {
			persister.delete( id, version, instance, session );
		}
		
		//postDelete:
		// After actually deleting a row, record the fact that the instance no longer 
		// exists on the database (needed for identity-column key generation), and
		// remove it from the session cache
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		EntityEntry entry = persistenceContext.removeEntry( instance );
		if ( entry == null ) {
			throw new AssertionFailure( "possible nonthreadsafe access to session" );
		}
		entry.postDelete();

		persistenceContext.removeEntity( entry.getEntityKey() );
		//persistenceContext.removeProxy( entry.getEntityKey() );
		
		//persistenceContext.getNaturalIdHelper().removeSharedNaturalIdCrossReference( persister, id, naturalIdValues );

		postDelete();

		if ( getSession().getFactory().getStatistics().isStatisticsEnabled() && !veto ) {
			getSession().getFactory().getStatisticsImplementor().deleteEntity( getPersister().getEntityName() );
		}
	}

	private boolean preDelete() {
		boolean veto = false;
//		EventListenerGroup<PreDeleteEventListener> listenerGroup = listenerGroup( EventType.PRE_DELETE );
//		if ( listenerGroup.isEmpty() ) {
//			return veto;
//		}
//		final PreDeleteEvent event = new PreDeleteEvent( getInstance(), getId(), state, getPersister(), eventSource() );
//		for ( PreDeleteEventListener listener : listenerGroup.listeners() ) {
//			veto |= listener.onPreDelete( event );
//		}
		return veto;
	}

	private void postDelete() {
		EventListenerGroup<PostDeleteEventListener> listenerGroup = listenerGroup( EventType.POST_DELETE );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PostDeleteEvent event = new PostDeleteEvent(
				getInstance(),
				getId(),
				state,
				getPersister(),
				eventSource()
		);
		for ( PostDeleteEventListener listener : listenerGroup.listeners() ) {
			listener.onPostDelete( event );
		}
	}

	private void postCommitDelete() {
		EventListenerGroup<PostDeleteEventListener> listenerGroup = listenerGroup( EventType.POST_COMMIT_DELETE );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PostDeleteEvent event = new PostDeleteEvent(
				getInstance(),
				getId(),
				state,
				getPersister(),
				eventSource()
		);
		for( PostDeleteEventListener listener : listenerGroup.listeners() ){
			listener.onPostDelete( event );
		}
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SessionImplementor session) throws ZormException {
		postCommitDelete();
	}

	@Override
	protected boolean hasPostCommitEventListeners() {
		return ! listenerGroup( EventType.POST_COMMIT_DELETE ).isEmpty();
	}
}
