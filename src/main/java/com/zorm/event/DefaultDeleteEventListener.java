package com.zorm.event;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.LockMode;
import com.zorm.action.CascadingAction;
import com.zorm.action.EntityDeleteAction;
import com.zorm.engine.Cascade;
import com.zorm.engine.ForeignKeys;
import com.zorm.engine.Nullability;
import com.zorm.engine.PersistenceContext;
import com.zorm.engine.Status;
import com.zorm.entity.EntityEntry;
import com.zorm.entity.EntityKey;
import com.zorm.exception.TransientObjectException;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.tuple.Lifecycle;
import com.zorm.type.Type;
import com.zorm.util.IdentitySet;
import com.zorm.util.TypeHelper;
public class DefaultDeleteEventListener implements DeleteEventListener {

	private static final Log log = LogFactory.getLog(DefaultDeleteEventListener.class);

	public void onDelete(DeleteEvent event) throws ZormException {
		onDelete( event, new IdentitySet() );
	}

	/**
	 * Handle the given delete event.  This is the cascaded form.
	 *
	 * @param event The delete event.
	 * @param transientEntities The cache of entities already deleted
	 *
	 * @throws ZormException
	 */
	public void onDelete(DeleteEvent event, Set transientEntities) throws ZormException {

		final EventSource source = event.getSession();

		final PersistenceContext persistenceContext = source.getPersistenceContext();
		Object entity = persistenceContext.unproxyAndReassociate( event.getObject() );

		EntityEntry entityEntry = persistenceContext.getEntry( entity );
		final EntityPersister persister;
		final Serializable id;
		final Object version;

		if ( entityEntry == null ) {

			persister = source.getEntityPersister( event.getEntityName(), entity );

			performDetachedEntityDeletionCheck( event );

			id = persister.getIdentifier( entity, source );

			if ( id == null ) {
				throw new TransientObjectException(
						"the detached instance passed to delete() had a null identifier"
				);
			}

			final EntityKey key = source.generateEntityKey( id, persister );

			persistenceContext.checkUniqueness( key, entity );

			//new OnUpdateVisitor( source, id, entity ).process( entity, persister );

			version = persister.getVersion( entity );

			entityEntry = persistenceContext.addEntity(
					entity,
					( persister.isMutable() ? Status.MANAGED : Status.READ_ONLY ),
					persister.getPropertyValues( entity ),
					key,
					version,
					LockMode.NONE,
					true,
					persister,
					false,
					false
			);
		}
		else {
			log.info("Deleting a persistent instance");
			
			if ( entityEntry.getStatus() == Status.DELETED || entityEntry.getStatus() == Status.GONE ) {
				return;
			}
			persister = entityEntry.getPersister();
			id = entityEntry.getId();
			version = entityEntry.getVersion();
		}

		if ( invokeDeleteLifecycle( source, entity, persister ) ) {
			return;
		}

		deleteEntity( source, entity, entityEntry, event.isCascadeDeleteEnabled(), persister, transientEntities );

		if ( source.getFactory().getSettings().isIdentifierRollbackEnabled() ) {
			persister.resetIdentifier( entity, id, version, source );
		}
	}

	/**
	 * Called when we have recognized an attempt to delete a detached entity.
	 * <p/>
	 * This is perfectly valid in Zorm usage; JPA, however, forbids this.
	 * Thus, this is a hook for HEM to affect this behavior.
	 *
	 * @param event The event.
	 */
	protected void performDetachedEntityDeletionCheck(DeleteEvent event) {
		// ok in normal Zorm usage to delete a detached entity; JPA however
		// forbids it, thus this is a hook for HEM to affect this behavior
	}

	/**
	 * We encountered a delete request on a transient instance.
	 * <p/>
	 * This is a deviation from historical Zorm (pre-3.2) behavior to
	 * align with the JPA spec, which states that transient entities can be
	 * passed to remove operation in which case cascades still need to be
	 * performed.
	 *
	 * @param session The session which is the source of the event
	 * @param entity The entity being delete processed
	 * @param cascadeDeleteEnabled Is cascading of deletes enabled
	 * @param persister The entity persister
	 * @param transientEntities A cache of already visited transient entities
	 * (to avoid infinite recursion).
	 */
	protected void deleteTransientEntity(
			EventSource session,
			Object entity,
			boolean cascadeDeleteEnabled,
			EntityPersister persister,
			Set transientEntities) {
		if ( transientEntities.contains( entity ) ) {
			return;
		}
		transientEntities.add( entity );
	}

	/**
	 * Perform the entity deletion.  Well, as with most operations, does not
	 * really perform it; just schedules an action/execution with the
	 * {@link org.Zorm.engine.spi.ActionQueue} for execution during flush.
	 *
	 * @param session The originating session
	 * @param entity The entity to delete
	 * @param entityEntry The entity's entry in the {@link PersistenceContext}
	 * @param isCascadeDeleteEnabled Is delete cascading enabled?
	 * @param persister The entity persister.
	 * @param transientEntities A cache of already deleted entities.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected final void deleteEntity(
			final EventSource session,
			final Object entity,
			final EntityEntry entityEntry,
			final boolean isCascadeDeleteEnabled,
			final EntityPersister persister,
			final Set transientEntities) {


		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final Type[] propTypes = persister.getPropertyTypes();
		final Object version = entityEntry.getVersion();

		final Object[] currentState;
		if ( entityEntry.getLoadedState() == null ) { //ie. the entity came in from update()
			currentState = persister.getPropertyValues( entity );
		}
		else {
			currentState = entityEntry.getLoadedState();
		}

		final Object[] deletedState = createDeletedState( persister, currentState, session );
		entityEntry.setDeletedState( deletedState );

		session.getInterceptor().onDelete(
				entity,
				entityEntry.getId(),
				deletedState,
				persister.getPropertyNames(),
				propTypes
		);

		persistenceContext.setEntryStatus( entityEntry, Status.DELETED );
		final EntityKey key = session.generateEntityKey( entityEntry.getId(), persister );

		cascadeBeforeDelete( session, persister, entity, entityEntry, transientEntities );
		
		new ForeignKeys.Nullifier( entity, true, false, session )
				.nullifyTransientReferences( entityEntry.getDeletedState(), propTypes );
		new Nullability( session ).checkNullability( entityEntry.getDeletedState(), persister, true );
		persistenceContext.getNullifiableEntityKeys().add( key );

		session.getActionQueue().addAction(
				new EntityDeleteAction(
						entityEntry.getId(),
						deletedState,
						version,
						entity,
						persister,
						isCascadeDeleteEnabled,
						session
				)
		);
		
		cascadeAfterDelete( session, persister, entity, transientEntities );
	}
	
	@SuppressWarnings("rawtypes")
	protected void cascadeBeforeDelete(
			EventSource session,
			EntityPersister persister,
			Object entity,
			EntityEntry entityEntry,
			Set transientEntities) throws ZormException {

		session.getPersistenceContext().incrementCascadeLevel();
		try {
			new Cascade( CascadingAction.DELETE, Cascade.AFTER_INSERT_BEFORE_DELETE, session )
					.cascade( persister, entity, transientEntities );
		}
		finally {
			session.getPersistenceContext().decrementCascadeLevel();
		}
	}
	
	@SuppressWarnings("rawtypes")
	protected void cascadeAfterDelete(
			EventSource session,
			EntityPersister persister,
			Object entity,
			Set transientEntities) throws ZormException {

		session.getPersistenceContext().incrementCascadeLevel();
		try {
			new Cascade( CascadingAction.DELETE, Cascade.BEFORE_INSERT_AFTER_DELETE, session )
					.cascade( persister, entity, transientEntities );
		}
		finally {
			session.getPersistenceContext().decrementCascadeLevel();
		}
	}

	private Object[] createDeletedState(EntityPersister persister, Object[] currentState, EventSource session) {
		Type[] propTypes = persister.getPropertyTypes();
		final Object[] deletedState = new Object[propTypes.length];
		boolean[] copyability = new boolean[propTypes.length];
		java.util.Arrays.fill( copyability, true );
		TypeHelper.deepCopy( currentState, propTypes, copyability, deletedState, session );
		return deletedState;
	}

	protected boolean invokeDeleteLifecycle(EventSource session, Object entity, EntityPersister persister) {
		if ( persister.implementsLifecycle() ) {
			if ( ( ( Lifecycle ) entity ).onDelete( session ) ) {
				return true;
			}
		}
		return false;
	}

}

