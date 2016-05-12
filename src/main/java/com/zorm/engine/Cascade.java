package com.zorm.engine;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import com.zorm.action.CascadingAction;
import com.zorm.collection.PersistentCollection;
import com.zorm.entity.EntityEntry;
import com.zorm.event.EventSource;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.persister.entity.EntityPersister;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.type.AssociationType;
import com.zorm.type.CollectionType;
import com.zorm.type.Type;
import com.zorm.type.EntityType;;

public final class Cascade {
	/**
	 * A cascade point that occurs just after the insertion of the parent entity and
	 * just before deletion
	 */
	public static final int AFTER_INSERT_BEFORE_DELETE = 1;
	/**
	 * A cascade point that occurs just before the insertion of the parent entity and
	 * just after deletion
	 */
	public static final int BEFORE_INSERT_AFTER_DELETE = 2;
	/**
	 * A cascade point that occurs just after the insertion of the parent entity and
	 * just before deletion, inside a collection
	 */
	public static final int AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION = 3;
	/**
	 * A cascade point that occurs just after update of the parent entity
	 */
	public static final int AFTER_UPDATE = 0;
	/**
	 * A cascade point that occurs just before the session is flushed
	 */
	public static final int BEFORE_FLUSH = 0;
	/**
	 * A cascade point that occurs just after eviction of the parent entity from the
	 * session cache
	 */
	public static final int AFTER_EVICT = 0;
	/**
	 * A cascade point that occurs just after locking a transient parent entity into the
	 * session cache
	 */
	public static final int BEFORE_REFRESH = 0;
	/**
	 * A cascade point that occurs just after refreshing a parent entity
	 */
	public static final int AFTER_LOCK = 0;
	/**
	 * A cascade point that occurs just before merging from a transient parent entity into
	 * the object in the session cache
	 */
	public static final int BEFORE_MERGE = 0;
	
	private int cascadeTo;
	private EventSource eventSource;
	private CascadingAction action;

	public Cascade(final CascadingAction action, final int cascadeTo, final EventSource eventSource) {
		this.cascadeTo = cascadeTo;
		this.eventSource = eventSource;
		this.action = action;
	}
	
	private SessionFactoryImplementor getFactory() {
		return eventSource.getFactory();
	}
	
	private boolean cascadeAssociationNow(AssociationType associationType) {
		return associationType.getForeignKeyDirection().cascadeNow(cascadeTo);
	}
	
	private void cascadeToOne(
			final Object parent,
			final Object child,
			final Type type,
			final CascadeStyle style,
			final Object anything,
			final boolean isCascadeDeleteEnabled) {
		final String entityName = type.isEntityType()
				? ( (EntityType) type ).getAssociatedEntityName()
				: null;
		if ( style.reallyDoCascade(action) ) { //not really necessary, but good for consistency...
			eventSource.getPersistenceContext().addChildParent(child, parent);
			try {
				action.cascade(eventSource, child, entityName, anything, isCascadeDeleteEnabled);
			}
			finally {
				eventSource.getPersistenceContext().removeChildParent(child);
			}
		}
	}
	
	private void cascadeAssociation(
			final Object parent,
			final Object child,
			final Type type,
			final CascadeStyle style,
			final Object anything,
			final boolean isCascadeDeleteEnabled) {
		if ( type.isEntityType() || type.isAnyType() ) {
			cascadeToOne( parent, child, type, style, anything, isCascadeDeleteEnabled );
		}
		else if ( type.isCollectionType() ) {
			cascadeCollection( parent, child, style, anything, (CollectionType) type );
		}
	}

	private void cascadeCollection(
			final Object parent,
			final Object child,
			final CascadeStyle style,
			final Object anything,
			final CollectionType type) {
		CollectionPersister persister = eventSource.getFactory()
				.getCollectionPersister( type.getRole() );
		Type elemType = persister.getElementType();

		final int oldCascadeTo = cascadeTo;
		if ( cascadeTo==AFTER_INSERT_BEFORE_DELETE) {
			cascadeTo = AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION;
		}

		if ( elemType.isEntityType() || elemType.isAnyType() || elemType.isComponentType() ) {
			cascadeCollectionElements(
				parent,
				child,
				type,
				style,
				elemType,
				anything,
				persister.isCascadeDeleteEnabled()
			);
		}

		cascadeTo = oldCascadeTo;
	}
	
	@SuppressWarnings("rawtypes")
	private void cascadeCollectionElements(
			final Object parent,
			final Object child,
			final CollectionType collectionType,
			final CascadeStyle style,
			final Type elemType,
			final Object anything,
			final boolean isCascadeDeleteEnabled) throws ZormException {

		boolean reallyDoCascade = style.reallyDoCascade(action) && child!=CollectionType.UNFETCHED_COLLECTION;

		if ( reallyDoCascade ) {
			Iterator iter = action.getCascadableChildrenIterator(eventSource, collectionType, child);
			while ( iter.hasNext() ) {
				cascadeProperty(
						parent,
						iter.next(),
						elemType,
						style,
						null,
						anything,
						isCascadeDeleteEnabled
					);
			}
		}

		final boolean deleteOrphans = style.hasOrphanDelete() &&
				action.deleteOrphans() &&
				elemType.isEntityType() &&
				child instanceof PersistentCollection; 

		if ( deleteOrphans ) { // handle orphaned entities!!
			final String entityName = collectionType.getAssociatedEntityName( eventSource.getFactory() );
			deleteOrphans( entityName, (PersistentCollection) child );
		}
	}
	
	private void deleteOrphans(String entityName, PersistentCollection pc) throws ZormException {
		final Collection orphans;
		if ( pc.wasInitialized() ) {
			CollectionEntry ce = eventSource.getPersistenceContext().getCollectionEntry(pc);
			orphans = ce==null ?
					java.util.Collections.EMPTY_LIST :
					ce.getOrphans(entityName, pc);
		}
		else {
			orphans = pc.getQueuedOrphans(entityName);
		}

		final Iterator orphanIter = orphans.iterator();
		while ( orphanIter.hasNext() ) {
			Object orphan = orphanIter.next();
			if (orphan!=null) {
				eventSource.delete( entityName, orphan, false, new HashSet() );
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	private Stack componentPathStack = new Stack();
	
	private void cascadeProperty(
			final Object parent,
			final Object child,
			final Type type,
			final CascadeStyle style,
			final String propertyName,
			final Object anything,
			final boolean isCascadeDeleteEnabled) throws ZormException {

		//child为实体的关联实体的实例
		if (child!=null) {
			if ( type.isAssociationType() ) {
				AssociationType associationType = (AssociationType) type;
				if ( cascadeAssociationNow( associationType ) ) {
					cascadeAssociation(
							parent,
							child,
							type,
							style,
							anything,
							isCascadeDeleteEnabled
						);
				}
			}
		}
		else {
			if ( isLogicalOneToOne( type ) ) {
				if ( style.hasOrphanDelete() && action.deleteOrphans() ) {
					final EntityEntry entry = eventSource.getPersistenceContext().getEntry( parent );
					if ( entry != null && entry.getStatus() != Status.SAVING ) {
						final Object loadedValue;
						if ( componentPathStack.isEmpty() ) {
							// association defined on entity
							loadedValue = entry.getLoadedValue( propertyName );
						}
						else {
							loadedValue = null;
						}
						if ( loadedValue != null ) {
							final EntityEntry valueEntry = eventSource
									.getPersistenceContext().getEntry( 
											loadedValue );
							final String entityName = valueEntry.getPersister().getEntityName();
							eventSource.delete( entityName, loadedValue, false, new HashSet() );
						}
					}
				}
			}
		}
	}
	
	private boolean isLogicalOneToOne(Type type) {
		return type.isEntityType() && ( (EntityType) type ).isLogicalOneToOne();
	}
	
	public void cascade(final EntityPersister persister, final Object parent)
			throws ZormException {
		cascade( persister, parent, null );
	}
	
	public void cascade(
			final EntityPersister persister, 
			final Object parent, 
			final Object anything) throws ZormException {

		if ( persister.hasCascades() || action.requiresNoCascadeChecking() ) { // performance opt
            //实体属性的类型
			Type[] types = persister.getPropertyTypes();
			//实体每个属性的级联类型
			CascadeStyle[] cascadeStyles = persister.getPropertyCascadeStyles();
			boolean hasUninitializedLazyProperties = persister.hasUninitializedLazyProperties( parent );
			for ( int i=0; i<types.length; i++) {
				final CascadeStyle style = cascadeStyles[i];
				final String propertyName = persister.getPropertyNames()[i];
				if ( hasUninitializedLazyProperties && persister.getPropertyLaziness()[i] && ! action.performOnLazyProperty() ) {
					continue;
				}

				if ( style.doCascade( action ) ) {
					cascadeProperty(
						    parent,
					        persister.getPropertyValue( parent, i ),
					        types[i],
					        style,
							propertyName,
					        anything,
					        false
					);
				}
				else if ( action.requiresNoCascadeChecking() ) {
					action.noCascade(
							eventSource,
							persister.getPropertyValue( parent, i ),
							parent,
							persister,
							i
					);
				}
			}
		}
	}
}
