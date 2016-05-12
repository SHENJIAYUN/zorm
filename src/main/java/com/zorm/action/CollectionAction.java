package com.zorm.action;

import java.io.Serializable;

import com.zorm.collection.PersistentCollection;
import com.zorm.event.EventListenerGroup;
import com.zorm.event.EventListenerRegistry;
import com.zorm.event.EventSource;
import com.zorm.event.EventType;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionImplementor;
import com.zorm.util.MessageHelper;
import com.zorm.util.StringHelper;

@SuppressWarnings("rawtypes")
public abstract class CollectionAction implements Executable, Serializable, Comparable{

	private static final long serialVersionUID = -518518443826556562L;
	private transient CollectionPersister persister;
	private transient SessionImplementor session;
	private final PersistentCollection collection;

	private final Serializable key;
	private final String collectionRole;

	public CollectionAction(
			final CollectionPersister persister, 
			final PersistentCollection collection, 
			final Serializable key, 
			final SessionImplementor session) {
		this.persister = persister;
		this.session = session;
		this.key = key;
		this.collectionRole = persister.getRole();
		this.collection = collection;
	}
	
	protected PersistentCollection getCollection() {
		return collection;
	}
	
	@Override
	public Serializable[] getPropertySpaces() {
		return persister.getCollectionSpaces();
	}

	protected final CollectionPersister getPersister() {
		return persister;
	}
	
	protected final Serializable getKey() {
		Serializable finalKey = key;
		if ( key instanceof DelayedPostInsertIdentifier ) {
			finalKey = session.getPersistenceContext().getEntry( collection.getOwner() ).getId();
			if ( finalKey == key ) {
			}
		}
		return finalKey;
	}

	protected final SessionImplementor getSession() {
		return session;
	}
	
	@Override
	public final void beforeExecutions() throws ZormException {
	}
	
	private AfterTransactionCompletionProcess afterTransactionProcess;

	@Override
	public AfterTransactionCompletionProcess getAfterTransactionCompletionProcess() {
		return afterTransactionProcess;
	}
	
	@Override
	public BeforeTransactionCompletionProcess getBeforeTransactionCompletionProcess() {
		return null;
	}
	
	@Override
	public int compareTo(Object other) {
		CollectionAction action = ( CollectionAction ) other;
		//sort first by role name
		int roleComparison = collectionRole.compareTo( action.collectionRole );
		if ( roleComparison != 0 ) {
			return roleComparison;
		}
		else {
			return persister.getKeyType()
					.compare( key, action.key );
		}
	}
	
	protected <T> EventListenerGroup<T> listenerGroup(EventType<T> eventType) {
		return getSession()
				.getFactory()
				.getServiceRegistry()
				.getService( EventListenerRegistry.class )
				.getEventListenerGroup( eventType );
	}

	protected EventSource eventSource() {
		return (EventSource) getSession();
	}
	
	@Override
	public String toString() {
		return StringHelper.unqualify( getClass().getName() ) + 
				MessageHelper.infoString( collectionRole, key );
	}

}
