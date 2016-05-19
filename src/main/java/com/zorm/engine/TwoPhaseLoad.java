package com.zorm.engine;

import java.io.Serializable;

import com.zorm.LazyPropertyInitializer;
import com.zorm.LockMode;
import com.zorm.entity.EntityEntry;
import com.zorm.entity.EntityKey;
import com.zorm.event.EventListenerGroup;
import com.zorm.event.EventListenerRegistry;
import com.zorm.event.EventType;
import com.zorm.event.PostLoadEvent;
import com.zorm.event.PostLoadEventListener;
import com.zorm.event.PreLoadEvent;
import com.zorm.event.PreLoadEventListener;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.property.BackrefPropertyAccessor;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;
import com.zorm.util.TypeHelper;

public final class TwoPhaseLoad {

	private TwoPhaseLoad() {}
	
	public static void addUninitializedEntity(
			final EntityKey key,
			final Object object,
			final EntityPersister persister,
			final LockMode lockMode,
			final boolean lazyPropertiesAreUnfetched,
			final SessionImplementor session
	) {
		session.getPersistenceContext().addEntity(
				object,
				Status.LOADING,
				null,
				key,
				null,
				lockMode,
				true,
				persister,
				false,
				lazyPropertiesAreUnfetched
			);
	}
	
	public static void postHydrate(
			final EntityPersister persister,
			final Serializable id,
			final Object[] values,
			final Object rowId,
			final Object object,
			final LockMode lockMode,
			final boolean lazyPropertiesAreUnfetched,
			final SessionImplementor session)
		throws ZormException {

			Object version = Versioning.getVersion( values, persister );
			session.getPersistenceContext().addEntry(
					object,
					Status.LOADING,
					values,
					rowId,
					id,
					version,
					lockMode,
					true,
					persister,
					false,
					lazyPropertiesAreUnfetched
				);

		}

	public static void initializeEntity(
			final Object entity,
			final boolean readOnly,
			final SessionImplementor session,
			final PreLoadEvent preLoadEvent,
			final PostLoadEvent postLoadEvent) throws ZormException {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final EntityEntry entityEntry = persistenceContext.getEntry(entity);

		doInitializeEntity( entity, entityEntry, readOnly, session, preLoadEvent, postLoadEvent );
	}

	private static void doInitializeEntity(
			final Object entity,
			final EntityEntry entityEntry,
			final boolean readOnly,
			final SessionImplementor session,
			final PreLoadEvent preLoadEvent,
			final PostLoadEvent postLoadEvent) throws ZormException {
		if ( entityEntry == null ) {
			throw new AssertionFailure( "possible non-threadsafe access to the session" );
		}

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		EntityPersister persister = entityEntry.getPersister();
		Serializable id = entityEntry.getId();
		Object[] hydratedState = entityEntry.getLoadedState();

		Type[] types = persister.getPropertyTypes();
		for ( int i = 0; i < hydratedState.length; i++ ) {
			final Object value = hydratedState[i];
			if ( value!=LazyPropertyInitializer.UNFETCHED_PROPERTY && value!=BackrefPropertyAccessor.UNKNOWN ) {
				hydratedState[i] = types[i].resolve( value, session, entity );
			}
		}

		//Must occur after resolving identifiers!
		if ( session.isEventSource() ) {
			preLoadEvent.setEntity( entity ).setState( hydratedState ).setId( id ).setPersister( persister );

			final EventListenerGroup<PreLoadEventListener> listenerGroup = session
					.getFactory()
					.getServiceRegistry()
					.getService( EventListenerRegistry.class )
					.getEventListenerGroup( EventType.PRE_LOAD );
			for ( PreLoadEventListener listener : listenerGroup.listeners() ) {
				listener.onPreLoad( preLoadEvent );
			}
		}

		persister.setPropertyValues( entity, hydratedState );

		final SessionFactoryImplementor factory = session.getFactory();

		boolean isReallyReadOnly = readOnly;
		if ( isReallyReadOnly ) {
			persistenceContext.setEntryStatus(entityEntry, Status.READ_ONLY);
		}
		else {
			TypeHelper.deepCopy(
					hydratedState,
					persister.getPropertyTypes(),
					persister.getPropertyUpdateability(),
					hydratedState,  //after setting values to object, entityMode
					session
			);
			persistenceContext.setEntryStatus(entityEntry, Status.MANAGED);
		}

		persister.afterInitialize(
				entity,
				entityEntry.isLoadedWithLazyPropertiesUnfetched(),
				session
		);
	}

	public static void postLoad(
			final Object entity,
			final SessionImplementor session,
			final PostLoadEvent postLoadEvent) {
		
		if ( session.isEventSource() ) {
			final PersistenceContext persistenceContext
					= session.getPersistenceContext();
			final EntityEntry entityEntry = persistenceContext.getEntry(entity);
			final Serializable id = entityEntry.getId();
			
			postLoadEvent.setEntity( entity ).setId( entityEntry.getId() )
					.setPersister( entityEntry.getPersister() );

			final EventListenerGroup<PostLoadEventListener> listenerGroup
					= session
							.getFactory()
							.getServiceRegistry()
							.getService( EventListenerRegistry.class )
							.getEventListenerGroup( EventType.POST_LOAD );
			for ( PostLoadEventListener listener : listenerGroup.listeners() ) {
				listener.onPostLoad( postLoadEvent );
			}
		}
	}
	
}
