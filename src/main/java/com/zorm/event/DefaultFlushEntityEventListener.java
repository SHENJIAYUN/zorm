package com.zorm.event;

import java.io.Serializable;
import java.util.Arrays;

import com.zorm.CustomEntityDirtinessStrategy;
import com.zorm.action.DelayedPostInsertIdentifier;
import com.zorm.action.EntityUpdateAction;
import com.zorm.engine.Nullability;
import com.zorm.engine.Status;
import com.zorm.engine.Versioning;
import com.zorm.entity.EntityEntry;
import com.zorm.entity.EntityKey;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.session.Session;
import com.zorm.session.SessionImplementor;
import com.zorm.type.Type;
import com.zorm.util.ArrayHelper;

public class DefaultFlushEntityEventListener implements
		FlushEntityEventListener {

	public void checkId(Object object, EntityPersister persister, Serializable id, SessionImplementor session)
			throws ZormException {

		if ( id != null && id instanceof DelayedPostInsertIdentifier ) {
			// this is a situation where the entity id is assigned by a post-insert generator
			// and was saved outside the transaction forcing it to be delayed
			return;
		}

		if ( persister.canExtractIdOutOfEntity() ) {

			Serializable oid = persister.getIdentifier( object, session );
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

	private void checkNaturalId(
			EntityPersister persister,
	        EntityEntry entry,
	        Object[] current,
	        Object[] loaded,
	        SessionImplementor session) {
		if ( persister.hasNaturalIdentifier() && entry.getStatus() != Status.READ_ONLY ) {
			if ( ! persister.getEntityMetamodel().hasImmutableNaturalId() ) {
				// SHORT-CUT: if the natural id is mutable (!immutable), no need to do the below checks
				// EARLY EXIT!!!
				return;
			}

			final int[] naturalIdentifierPropertiesIndexes = persister.getNaturalIdentifierProperties();
			final Type[] propertyTypes = persister.getPropertyTypes();
			final boolean[] propertyUpdateability = persister.getPropertyUpdateability();

//			final Object[] snapshot = loaded == null
//					? session.getPersistenceContext().getNaturalIdSnapshot( entry.getId(), persister )
//					: session.getPersistenceContext().getNaturalIdHelper().extractNaturalIdValues( loaded, persister );

			for ( int i=0; i<naturalIdentifierPropertiesIndexes.length; i++ ) {
				final int naturalIdentifierPropertyIndex = naturalIdentifierPropertiesIndexes[i];
				if ( propertyUpdateability[ naturalIdentifierPropertyIndex ] ) {
					// if the given natural id property is updatable (mutable), there is nothing to check
					continue;
				}

//				final Type propertyType = propertyTypes[naturalIdentifierPropertyIndex];
//				if ( ! propertyType.isEqual( current[naturalIdentifierPropertyIndex], snapshot[i] ) ) {
//					throw new ZormException(
//							String.format(
//									"An immutable natural identifier of entity %s was altered from %s to %s",
//									persister.getEntityName(),
//									propertyTypes[naturalIdentifierPropertyIndex].toLoggableString(
//											snapshot[i],
//											session.getFactory()
//									),
//									propertyTypes[naturalIdentifierPropertyIndex].toLoggableString(
//											current[naturalIdentifierPropertyIndex],
//											session.getFactory()
//									)
//							)
//					);
//			   }
			}
		}
	}

	/**
	 * Flushes a single entity's state to the database, by scheduling
	 * an update action, if necessary
	 */
	public void onFlushEntity(FlushEntityEvent event) throws ZormException {
		//获取要持久化的实例
		final Object entity = event.getEntity();
		final EntityEntry entry = event.getEntityEntry();
		final EventSource session = event.getSession();
		final EntityPersister persister = entry.getPersister();
		final Status status = entry.getStatus();
		final Type[] types = persister.getPropertyTypes();

		final boolean mightBeDirty = entry.requiresDirtyCheck(entity);

		final Object[] values = getValues( entity, entry, mightBeDirty, session );

		event.setPropertyValues(values);

		//TODO: avoid this for non-new instances where mightBeDirty==false
		boolean substitute = wrapCollections( session, persister, types, values);

		if ( isUpdateNecessary( event, mightBeDirty ) ) {
			//需要更新时，会往动作队列中加入更新动作
			substitute = scheduleUpdate( event ) || substitute;
		}

		if ( status != Status.DELETED ) {
			if (substitute) persister.setPropertyValues( entity, values );

			if ( persister.hasCollections() ) {
				new FlushVisitor(session, entity).processEntityPropertyValues(values, types);
			}
		}

	}

	private Object[] getValues(Object entity, EntityEntry entry, boolean mightBeDirty, SessionImplementor session) {
		final Object[] loadedState = entry.getLoadedState();
		final Status status = entry.getStatus();
		final EntityPersister persister = entry.getPersister();

		final Object[] values;
		if ( status == Status.DELETED ) {
			//grab its state saved at deletion
			values = entry.getDeletedState();
		}
		else if ( !mightBeDirty && loadedState!=null ) {
			values = loadedState;
		}
		else {
			checkId( entity, persister, entry.getId(), session );

			// grab its current state
			values = persister.getPropertyValues( entity );

			checkNaturalId( persister, entry, values, loadedState, session );
		}
		return values;
	}

	private boolean wrapCollections(
			EventSource session,
			EntityPersister persister,
			Type[] types,
			Object[] values
	) {
		if ( persister.hasCollections() ) {

			WrapVisitor visitor = new WrapVisitor(session);
			visitor.processEntityPropertyValues(values, types);
			return visitor.isSubstitutionRequired();
		}
		else {
			return false;
		}
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
				event.getSession()
						.getFactory()
						.getCustomEntityDirtinessStrategy()
						.resetDirty( event.getEntity(), event.getEntityEntry().getPersister(), event.getSession() );
				return false;
			}
		}
		else {
			return hasDirtyCollections( event, event.getEntityEntry().getPersister(), status );
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

		// increment the version number (if necessary)
		final Object nextVersion = getNextVersion(event);

		// if it was dirtied by a collection only
		int[] dirtyProperties = event.getDirtyProperties();
		if ( event.isDirtyCheckPossible() && dirtyProperties == null ) {
			if ( ! intercepted && !event.hasDirtyCollection() ) {
				throw new AssertionFailure( "dirty, but no dirty properties" );
			}
			dirtyProperties = ArrayHelper.EMPTY_INT_ARRAY;
		}

		// check nullability but do not doAfterTransactionCompletion command execute
		// we'll use scheduled updates for that.
		new Nullability(session).checkNullability( values, persister, true );

		// schedule the update
		// note that we intentionally do _not_ pass in currentPersistentState!
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

	/**
	 * Convience method to retreive an entities next version value
	 */
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

	/**
	 * Performs all necessary checking to determine if an entity needs an SQL update
	 * to synchronize its state to the database. Modifies the event by side-effect!
	 * Note: this method is quite slow, avoid calling if possible!
	 */
	protected final boolean isUpdateNecessary(FlushEntityEvent event) throws ZormException {

		EntityPersister persister = event.getEntityEntry().getPersister();
		Status status = event.getEntityEntry().getStatus();

		if ( !event.isDirtyCheckPossible() ) {
			return true;
		}
		else {

			int[] dirtyProperties = event.getDirtyProperties();
			if ( dirtyProperties!=null && dirtyProperties.length!=0 ) {
				return true; //TODO: suck into event class
			}
			else {
				return hasDirtyCollections( event, persister, status );
			}

		}
	}

	private boolean hasDirtyCollections(FlushEntityEvent event, EntityPersister persister, Status status) {
		if ( isCollectionDirtyCheckNecessary(persister, status ) ) {
//			DirtyCollectionSearchVisitor visitor = new DirtyCollectionSearchVisitor(
//					event.getSession(),
//					persister.getPropertyVersionability()
//				);
//			visitor.processEntityPropertyValues( event.getPropertyValues(), persister.getPropertyTypes() );
//			boolean hasDirtyCollections = visitor.wasDirtyCollectionFound();
//			event.setHasDirtyCollection(hasDirtyCollections);
//			return hasDirtyCollections;
			return false;
		}
		else {
			return false;
		}
	}

	private boolean isCollectionDirtyCheckNecessary(EntityPersister persister, Status status) {
//		return ( status == Status.MANAGED || status == Status.READ_ONLY ) &&
//				persister.isVersioned() &&
//				persister.hasCollections();
		return false;
	}

	/**
	 * Perform a dirty check, and attach the results to the event
	 */
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
//				final Object[] databaseSnapshot = getDatabaseSnapshot(session, persister, id);
//				if ( databaseSnapshot != null ) {
//					dirtyProperties = persister.findModified(databaseSnapshot, values, entity, session);
//					cannotDirtyCheck = false;
//					event.setDatabaseSnapshot(databaseSnapshot);
//				}
			}
		}
		else {
			// the Interceptor handled the dirty checking
			cannotDirtyCheck = false;
			interceptorHandledDirtyCheck = true;
		}

		logDirtyProperties( id, dirtyProperties, persister );

		event.setDirtyProperties(dirtyProperties);
		event.setDirtyCheckHandledByInterceptor(interceptorHandledDirtyCheck);
		event.setDirtyCheckPossible(!cannotDirtyCheck);

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

	private void logDirtyProperties(Serializable id, int[] dirtyProperties, EntityPersister persister) {
		if ( dirtyProperties != null && dirtyProperties.length > 0 ) {
			final String[] allPropertyNames = persister.getPropertyNames();
			final String[] dirtyPropertyNames = new String[ dirtyProperties.length ];
			for ( int i = 0; i < dirtyProperties.length; i++ ) {
				dirtyPropertyNames[i] = allPropertyNames[ dirtyProperties[i]];
			}
		}
	}
}
