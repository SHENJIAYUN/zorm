package com.zorm.action;

import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zorm.LockMode;
import com.zorm.LockOptions;
import com.zorm.collection.PersistentCollection;
import com.zorm.event.EventSource;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.session.SessionImplementor;
import com.zorm.type.CollectionType;

public abstract class CascadingAction {
	private static final Log log = LogFactory.getLog(CascadingAction.class);

	public CascadingAction() {
	}

	/**
	 * Cascade the action to the child object.
	 *
	 * @param session
	 *            The session within which the cascade is occuring.
	 * @param child
	 *            The child to which cascading should be performed.
	 * @param entityName
	 *            The child's entity name
	 * @param anything
	 *            Anything ;) Typically some form of cascade-local cache which
	 *            is specific to each CascadingAction type
	 * @param isCascadeDeleteEnabled
	 *            Are cascading deletes enabled.
	 * @throws HibernateException
	 */
	public abstract void cascade(EventSource session, Object child,
			String entityName, Object anything, boolean isCascadeDeleteEnabled)
			throws ZormException;

	/**
	 * Given a collection, get an iterator of the children upon which the
	 * current cascading action should be visited.
	 *
	 * @param session
	 *            The session within which the cascade is occuring.
	 * @param collectionType
	 *            The mapping type of the collection.
	 * @param collection
	 *            The collection instance.
	 * @return The children iterator.
	 */
	public abstract Iterator getCascadableChildrenIterator(EventSource session,
			CollectionType collectionType, Object collection);

	/**
	 * Does this action potentially extrapolate to orphan deletes?
	 *
	 * @return True if this action can lead to deletions of orphans.
	 */
	public abstract boolean deleteOrphans();

	/**
	 * Does the specified cascading action require verification of no cascade
	 * validity?
	 *
	 * @return True if this action requires no-cascade verification; false
	 *         otherwise.
	 */
	public boolean requiresNoCascadeChecking() {
		return false;
	}

	/**
	 * Called (in the case of {@link #requiresNoCascadeChecking} returning true)
	 * to validate that no cascade on the given property is considered a valid
	 * semantic.
	 *
	 * @param session
	 *            The session witin which the cascade is occurring.
	 * @param child
	 *            The property value
	 * @param parent
	 *            The property value owner
	 * @param persister
	 *            The entity persister for the owner
	 * @param propertyIndex
	 *            The index of the property within the owner.
	 */
	public void noCascade(EventSource session, Object child, Object parent,
			EntityPersister persister, int propertyIndex) {
	}

	/**
	 * Should this action be performed (or noCascade consulted) in the case of
	 * lazy properties.
	 */
	public boolean performOnLazyProperty() {
		return true;
	}

	// the CascadingAction implementations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static final CascadingAction DELETE = new CascadingAction() {
		@SuppressWarnings("rawtypes")
		@Override
		public void cascade(EventSource session, Object child,String entityName, Object anything,boolean isCascadeDeleteEnabled) throws ZormException {
			log.info("Cascading to delete: "+entityName);
			session.delete(entityName, child, isCascadeDeleteEnabled, (Set) anything);
		}

		@Override
		public Iterator getCascadableChildrenIterator(EventSource session,
				CollectionType collectionType, Object collection) {
			return CascadingAction.getAllElementsIterator(session,
					collectionType, collection);
		}

		@Override
		public boolean deleteOrphans() {
			return true;
		}

		@Override
		public String toString() {
			return "ACTION_DELETE";
		}
	};

	public static final CascadingAction SAVE_UPDATE = new CascadingAction() {
		@Override
		public void cascade(EventSource session, Object child,
				String entityName, Object anything,
				boolean isCascadeDeleteEnabled) throws ZormException {
			log.debug("Cascading to save or update: "+entityName);
			session.saveOrUpdate(entityName, child);
		}

		@Override
		public Iterator getCascadableChildrenIterator(EventSource session,
				CollectionType collectionType, Object collection) {
			return getLoadedElementsIterator(session, collectionType,collection);
		}

		@Override
		public boolean deleteOrphans() {
			return true;
		}

		@Override
		public boolean performOnLazyProperty() {
			return false;
		}

		@Override
		public String toString() {
			return "ACTION_SAVE_UPDATE";
		}
	};

	private static boolean collectionIsInitialized(Object collection) {
		return !(collection instanceof PersistentCollection) || ( (PersistentCollection) collection ).wasInitialized();
	}
	
	public static Iterator getLoadedElementsIterator(SessionImplementor session, CollectionType collectionType, Object collection) {
		if ( collectionIsInitialized(collection) ) {
			// handles arrays and newly instantiated collections
			return collectionType.getElementsIterator(collection, session);
		}
		else {
			// does not handle arrays (thats ok, cos they can't be lazy)
			// or newly instantiated collections, so we can do the cast
			return ( (PersistentCollection) collection ).queuedAdditionIterator();
		}
	}
	
	private static Iterator getAllElementsIterator(EventSource session,
			CollectionType collectionType, Object collection) {
		return collectionType.getElementsIterator(collection, session);
	}

}
