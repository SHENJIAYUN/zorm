package com.zorm.event;

import java.io.Serializable;
import java.util.Arrays;

import com.zorm.CustomEntityDirtinessStrategy;
import com.zorm.action.EntityUpdateAction;
import com.zorm.engine.Nullability;
import com.zorm.engine.PersistenceContext;
import com.zorm.engine.Status;
import com.zorm.engine.Versioning;
import com.zorm.entity.EntityEntry;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.session.Session;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;
import com.zorm.util.ArrayHelper;

public class DefaultFlushEventListener extends AbstractFlushingEventListener implements FlushEventListener{

	private static final long serialVersionUID = -2236835087544328263L;

	@Override
	public void onFlush(FlushEvent event) throws ZormException {
		final EventSource source = event.getSession();
		final PersistenceContext persistenceContext = source.getPersistenceContext();
		if(persistenceContext.getEntityEntries().size() > 0 ||
				persistenceContext.getCollectionEntries().size() > 0){
			flushEverythingToExecutions(event);
			performExecutions(source);
			postFlush(source);
		}
	}

	public void onFlushEntity(FlushEntityEvent event) throws ZormException{
		final Object entity = event.getEntity();
		final EntityEntry entry = event.getEntityEntry();
		final EventSource session = event.getSession();
		final EntityPersister persister = entry.getPersister();
		final Status status = entry.getStatus();
		final Type[] types = persister.getPropertyTypes();
		
		final boolean mightBeDirty = entry.requiresDirtyCheck(entity);

		final Object[] values = getValues(entity,entry,mightBeDirty,session);
        event.setPropertyValues(values);
        
        boolean substitute = wrapCollections( session, persister, types, values);
        
        if ( isUpdateNecessary( event, mightBeDirty ) ) {
			substitute = scheduleUpdate( event ) || substitute;
		}
        
        if ( status != Status.DELETED ) {
			if (substitute) persister.setPropertyValues( entity, values );

		}
	}
	
	protected void dirtyCheck(final FlushEntityEvent event) throws ZormException {

		final Object entity = event.getEntity();
		final Object[] values = event.getPropertyValues();
		final SessionImplementor session = event.getSession();
		final EntityEntry entry = event.getEntityEntry();
		final EntityPersister persister = entry.getPersister();
		final Serializable id = entry.getId();
		final Object[] loadedState = entry.getLoadedState();

		int[] dirtyProperties = session.getInterceptor().findDirty(
				entity,
				id,
				values,
				loadedState,
				persister.getPropertyNames(),
				persister.getPropertyTypes()
		);

		if ( dirtyProperties == null ) {
			// see if the custom dirtiness strategy can tell us...
			class DirtyCheckContextImpl implements CustomEntityDirtinessStrategy.DirtyCheckContext {
				int[] found = null;
				@Override
				public void doDirtyChecking(CustomEntityDirtinessStrategy.AttributeChecker attributeChecker) {
					found = new DirtyCheckAttributeInfoImpl( event ).visitAttributes( attributeChecker );
					if ( found != null && found.length == 0 ) {
						found = null;
					}
				}
			}
			DirtyCheckContextImpl context = new DirtyCheckContextImpl();
			session.getFactory().getCustomEntityDirtinessStrategy().findDirty(
					entity,
					persister,
					(Session) session,
					context
			);
			dirtyProperties = context.found;
		}

		event.setDatabaseSnapshot(null);

		final boolean interceptorHandledDirtyCheck;
		boolean cannotDirtyCheck;

		if ( dirtyProperties==null ) {
			interceptorHandledDirtyCheck = false;

			cannotDirtyCheck = loadedState==null; // object loaded by update()
			if ( !cannotDirtyCheck ) {
				dirtyProperties = persister.findDirty( values, loadedState, entity, session );
			}
			else if ( entry.getStatus() == Status.DELETED && ! event.getEntityEntry().isModifiableEntity() ) {
				// A non-modifiable (e.g., read-only or immutable) entity needs to be have
				// references to transient entities set to null before being deleted. No other
				// fields should be updated.
				if ( values != entry.getDeletedState() ) {
					throw new IllegalStateException(
							"Entity has status Status.DELETED but values != entry.getDeletedState"
					);
				}
				final Object[] currentState = persister.getPropertyValues( event.getEntity() );
				dirtyProperties = persister.findDirty( entry.getDeletedState(), currentState, entity, session );
				cannotDirtyCheck = false;
			}
			else {
				// dirty check against the database snapshot, if possible/necessary
				final Object[] databaseSnapshot = getDatabaseSnapshot(session, persister, id);
				if ( databaseSnapshot != null ) {
					dirtyProperties = persister.findModified(databaseSnapshot, values, entity, session);
					cannotDirtyCheck = false;
					event.setDatabaseSnapshot(databaseSnapshot);
				}
			}
		}
		else {
			// the Interceptor handled the dirty checking
			cannotDirtyCheck = false;
			interceptorHandledDirtyCheck = true;
		}

		event.setDirtyProperties(dirtyProperties);
		event.setDirtyCheckHandledByInterceptor(interceptorHandledDirtyCheck);
		event.setDirtyCheckPossible(!cannotDirtyCheck);

	}
	
	private Object[] getDatabaseSnapshot(SessionImplementor session,
			EntityPersister persister, Serializable id) {
		return null;
	}
	
	protected final boolean isUpdateNecessary(FlushEntityEvent event){
		EntityPersister persister = event.getEntityEntry().getPersister();
		Status status = event.getEntityEntry().getStatus();
		
		if ( !event.isDirtyCheckPossible() ) {
			return true;
		}
		else{
			int[] dirtyProperties = event.getDirtyProperties();
			if ( dirtyProperties!=null && dirtyProperties.length!=0 ) {
				return true; //TODO: suck into event class
			}
			else {
				return hasDirtyCollections( event, persister, status );
			}
		}
	}
	

	private boolean hasDirtyCollections(FlushEntityEvent event,
			EntityPersister persister, Status status) {
		return false;
	}

	private boolean isUpdateNecessary(final FlushEntityEvent event, final boolean mightBeDirty) {
		final Status status = event.getEntityEntry().getStatus();
		if ( mightBeDirty || status==Status.DELETED ) {
			dirtyCheck(event);
			if ( isUpdateNecessary(event) ) {
				return true;
			}
			else {
//				if ( event.getEntityEntry().getPersister().getInstrumentationMetadata().isInstrumented() ) {
//					event.getEntityEntry()
//							.getPersister()
//							.getInstrumentationMetadata()
//							.extractInterceptor( event.getEntity() )
//							.clearDirty();
//				}
//				event.getSession()
//						.getFactory()
//						.getCustomEntityDirtinessStrategy()
//						.resetDirty( event.getEntity(), event.getEntityEntry().getPersister(), event.getSession() );
				return false;
			}
		}
		else {
			return hasDirtyCollections( event, event.getEntityEntry().getPersister(), status );
		}
	}
	
	private boolean wrapCollections(EventSource session,
			EntityPersister persister, Type[] types, Object[] values) {
		return false;
	}
	
	protected boolean invokeInterceptor(
			SessionImplementor session,
			Object entity,
			EntityEntry entry,
			final Object[] values,
			EntityPersister persister) {
		return session.getInterceptor().onFlushDirty(
				entity,
				entry.getId(),
				values,
				entry.getLoadedState(),
				persister.getPropertyNames(),
				persister.getPropertyTypes()
		);
	}
	
	protected boolean handleInterception(FlushEntityEvent event) {
		SessionImplementor session = event.getSession();
		EntityEntry entry = event.getEntityEntry();
		EntityPersister persister = entry.getPersister();
		Object entity = event.getEntity();

		//give the Interceptor a chance to modify property values
		final Object[] values = event.getPropertyValues();
		final boolean intercepted = invokeInterceptor( session, entity, entry, values, persister );

		//now we might need to recalculate the dirtyProperties array
		if ( intercepted && event.isDirtyCheckPossible() && !event.isDirtyCheckHandledByInterceptor() ) {
			int[] dirtyProperties;
			if ( event.hasDatabaseSnapshot() ) {
				dirtyProperties = persister.findModified( event.getDatabaseSnapshot(), values, entity, session );
			}
			else {
				dirtyProperties = persister.findDirty( values, entry.getLoadedState(), entity, session );
			}
			event.setDirtyProperties(dirtyProperties);
		}

		return intercepted;
	}

	private boolean isVersionIncrementRequired(
			FlushEntityEvent event,
			EntityEntry entry,
			EntityPersister persister,
			int[] dirtyProperties
	) {
		final boolean isVersionIncrementRequired = entry.getStatus()!=Status.DELETED && (
				dirtyProperties==null ||
				Versioning.isVersionIncrementRequired(
						dirtyProperties,
						event.hasDirtyCollection(),
						persister.getPropertyVersionability()
				)
			);
		return isVersionIncrementRequired;
	}
	
	private Object getNextVersion(FlushEntityEvent event) throws ZormException {

		EntityEntry entry = event.getEntityEntry();
		EntityPersister persister = entry.getPersister();
		if ( persister.isVersioned() ) {

			Object[] values = event.getPropertyValues();

			if ( entry.isBeingReplicated() ) {
				return Versioning.getVersion(values, persister);
			}
			else {
				int[] dirtyProperties = event.getDirtyProperties();

				final boolean isVersionIncrementRequired = isVersionIncrementRequired(
						event,
						entry,
						persister,
						dirtyProperties
					);

				final Object nextVersion = isVersionIncrementRequired ?
						Versioning.increment( entry.getVersion(), persister.getVersionType(), event.getSession() ) :
						entry.getVersion(); //use the current version

				Versioning.setVersion(values, nextVersion, persister);

				return nextVersion;
			}
		}
		else {
			return null;
		}

	}
	
	private boolean scheduleUpdate(final FlushEntityEvent event) {
		final EntityEntry entry = event.getEntityEntry();
		final EventSource session = event.getSession();
		final Object entity = event.getEntity();
		final Status status = entry.getStatus();
		final EntityPersister persister = entry.getPersister();
		final Object[] values = event.getPropertyValues();
		
		final boolean intercepted = !entry.isBeingReplicated() && handleInterception( event );
		
		final Object nextVersion = getNextVersion(event);
		int[] dirtyProperties = event.getDirtyProperties();
		
		if ( event.isDirtyCheckPossible() && dirtyProperties == null ) {
			if ( ! intercepted && !event.hasDirtyCollection() ) {
				throw new AssertionFailure( "dirty, but no dirty properties" );
			}
			dirtyProperties = ArrayHelper.EMPTY_INT_ARRAY;
		}
		
		new Nullability(session).checkNullability( values, persister, true );
		
		session.getActionQueue().addAction(
				new EntityUpdateAction(
						entry.getId(),
						values,
						dirtyProperties,
						event.hasDirtyCollection(),
						( status == Status.DELETED && ! entry.isModifiableEntity() ?
								persister.getPropertyValues( entity ) :
								entry.getLoadedState() ),
						entry.getVersion(),
						nextVersion,
						entity,
						entry.getRowId(),
						persister,
						session
					)
			);
		return intercepted;
	}
	

	private Object[] getValues(Object entity, EntityEntry entry,
			boolean mightBeDirty, SessionImplementor session) {
		final Object[] loadedState = entry.getLoadedState();
		final Status status = entry.getStatus();
		final EntityPersister persister = entry.getPersister();
		final Object[] values;
		
		checkId( entity, persister, entry.getId(), session );
		values = persister.getPropertyValues( entity );
		return values;
	}

	private void checkId(Object entity, EntityPersister persister,
			Serializable id, SessionImplementor session) {
		if ( persister.canExtractIdOutOfEntity() ) {

			Serializable oid = persister.getIdentifier( entity, session );
			if (id==null) {
				throw new AssertionFailure("null id in " + persister.getEntityName() + " entry (don't flush the Session after an exception occurs)");
			}
			if ( !persister.getIdentifierType().isEqual( id, oid, session.getFactory() ) ) {
				throw new ZormException(
						"identifier of an instance of " +
						persister.getEntityName() +
						" was altered from " + id +
						" to " + oid
					);
			}
		}
	}
	
	private Object[] getDatabaseSnapshot(EventSource session,
			EntityPersister persister2, Serializable id) {
		return null;
	}
	
	private class DirtyCheckAttributeInfoImpl implements CustomEntityDirtinessStrategy.AttributeInformation {
		private final FlushEntityEvent event;
		private final EntityPersister persister;
		private final int numberOfAttributes;
		private int index = 0;

		private DirtyCheckAttributeInfoImpl(FlushEntityEvent event) {
			this.event = event;
			this.persister = event.getEntityEntry().getPersister();
			this.numberOfAttributes = persister.getPropertyNames().length;
		}

		@Override
		public EntityPersister getContainingPersister() {
			return persister;
		}

		@Override
		public int getAttributeIndex() {
			return index;
		}

		@Override
		public String getName() {
			return persister.getPropertyNames()[ index ];
		}

		@Override
		public Type getType() {
			return persister.getPropertyTypes()[ index ];
		}

		@Override
		public Object getCurrentValue() {
			return event.getPropertyValues()[ index ];
		}

		Object[] databaseSnapshot;

		public int[] visitAttributes(CustomEntityDirtinessStrategy.AttributeChecker attributeChecker) {
			databaseSnapshot = null;
			index = 0;

			final int[] indexes = new int[ numberOfAttributes ];
			int count = 0;
			for ( ; index < numberOfAttributes; index++ ) {
				if ( attributeChecker.isDirty( this ) ) {
					indexes[ count++ ] = index;
				}
			}
			return Arrays.copyOf( indexes, count );
		}
	}

}
