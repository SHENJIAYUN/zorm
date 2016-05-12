package com.zorm.engine;

import java.io.Serializable;

import com.zorm.collection.PersistentCollection;
import com.zorm.entity.EntityEntry;
import com.zorm.entity.EntityKey;
import com.zorm.event.EventSource;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.type.CollectionType;

public final class Collections {
	private Collections() {
	}

	public static void processReachableCollection(
			PersistentCollection collection,
	        CollectionType type,
	        Object entity,
	        SessionImplementor session) {

		collection.setOwner(entity);

		CollectionEntry ce = session.getPersistenceContext().getCollectionEntry(collection);

		if ( ce == null ) {
			throw new ZormException(
					"Found two representations of same collection: " +
					type.getRole()
			);
		}
		if ( ce.isReached() ) {
			// We've been here before
			throw new ZormException(
					"Found shared references to a collection: " +
					type.getRole()
			);
		}
		ce.setReached(true);

		SessionFactoryImplementor factory = session.getFactory();
		CollectionPersister persister = factory.getCollectionPersister( type.getRole() );
		ce.setCurrentPersister(persister);
		ce.setCurrentKey( type.getKeyOfOwner(entity, session) ); //TODO: better to pass the id in as an argument?


		prepareCollectionForUpdate( collection, ce, factory );
     }
   
	private static void prepareCollectionForUpdate(
			PersistentCollection collection,
	        CollectionEntry entry,
	        SessionFactoryImplementor factory) {

		if ( entry.isProcessed() ) {
			throw new AssertionFailure( "collection was processed twice by flush()" );
		}
		entry.setProcessed( true );

		final CollectionPersister loadedPersister = entry.getLoadedPersister();
		final CollectionPersister currentPersister = entry.getCurrentPersister();
		if ( loadedPersister != null || currentPersister != null ) {					// it is or was referenced _somewhere_

			boolean ownerChanged = loadedPersister != currentPersister ||				// if either its role changed,
			                       !currentPersister
					                       .getKeyType().isEqual(                       // or its key changed
													entry.getLoadedKey(),
			                                        entry.getCurrentKey(),
			                                        factory
			                       );

			if (ownerChanged) {

				// do a check
				final boolean orphanDeleteAndRoleChanged = loadedPersister != null &&
				                                           currentPersister != null &&
				                                           loadedPersister.hasOrphanDelete();

				if (orphanDeleteAndRoleChanged) {
					throw new ZormException(
							"Don't change the reference to a collection with cascade=\"all-delete-orphan\": " +
							loadedPersister.getRole()
					);
				}

				// do the work
				if ( currentPersister != null ) {
					entry.setDorecreate( true );	// we will need to create new entries
				}

				if ( loadedPersister != null ) {
					entry.setDoremove( true );		// we will need to remove ye olde entries
					if ( entry.isDorecreate() ) {
						collection.forceInitialization();
					}
				}
			}
			else if ( collection.isDirty() ) {
				// the collection's elements have changed
				entry.setDoupdate( true );
			}

		}

	}

	public static void processUnreachableCollection(PersistentCollection coll, SessionImplementor session) {
		if ( coll.getOwner()==null ) {
			processNeverReferencedCollection(coll, session);
		}
		else {
			processDereferencedCollection(coll, session);
		}
	}
	
	private static void processDereferencedCollection(PersistentCollection coll, SessionImplementor session) {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		CollectionEntry entry = persistenceContext.getCollectionEntry(coll);
		final CollectionPersister loadedPersister = entry.getLoadedPersister();

		// do a check
		boolean hasOrphanDelete = loadedPersister != null && loadedPersister.hasOrphanDelete();
		if (hasOrphanDelete) {
			Serializable ownerId = loadedPersister.getOwnerEntityPersister().getIdentifier( coll.getOwner(), session );
			if ( ownerId == null ) {
				if ( session.getFactory().getSettings().isIdentifierRollbackEnabled() ) {
					EntityEntry ownerEntry = persistenceContext.getEntry( coll.getOwner() );
					if ( ownerEntry != null ) {
						ownerId = ownerEntry.getId();
					}
				}
				if ( ownerId == null ) {
					throw new AssertionFailure( "Unable to determine collection owner identifier for orphan-delete processing" );
				}
			}
			EntityKey key = session.generateEntityKey( ownerId, loadedPersister.getOwnerEntityPersister() );
			Object owner = persistenceContext.getEntity(key);
			if ( owner == null ) {
				throw new AssertionFailure(
						"collection owner not associated with session: " +
						loadedPersister.getRole()
				);
			}
			EntityEntry e = persistenceContext.getEntry(owner);
			//only collections belonging to deleted entities are allowed to be dereferenced in the case of orphan delete
			if ( e != null && e.getStatus() != Status.DELETED && e.getStatus() != Status.GONE ) {
				throw new ZormException(
						"A collection with cascade=\"all-delete-orphan\" was no longer referenced by the owning entity instance: " +
						loadedPersister.getRole()
				);
			}
		}

		// do the work
		entry.setCurrentPersister(null);
		entry.setCurrentKey(null);
		prepareCollectionForUpdate( coll, entry, session.getFactory() );

	}
	
	private static void processNeverReferencedCollection(PersistentCollection coll, SessionImplementor session)
			throws ZormException {

			final PersistenceContext persistenceContext = session.getPersistenceContext();
			CollectionEntry entry = persistenceContext.getCollectionEntry(coll);

			entry.setCurrentPersister( entry.getLoadedPersister() );
			entry.setCurrentKey( entry.getLoadedKey() );

			prepareCollectionForUpdate( coll, entry, session.getFactory() );

	}

	
}
