package com.zorm.collection;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import com.zorm.exception.ZormException;
import com.zorm.loader.CollectionAliases;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionImplementor;

public interface PersistentCollection {

	public boolean wasInitialized();
	
	@SuppressWarnings("rawtypes")
	public Iterator queuedAdditionIterator();

	public void clearDirty();

	public Serializable getSnapshot(CollectionPersister persister);

	public void setSnapshot(Serializable loadedKey, String role,Serializable snapshot);

	@SuppressWarnings("rawtypes")
	public Collection getOrphans(Serializable snapshot, String entityName);

	@SuppressWarnings("rawtypes")
	public Collection getQueuedOrphans(String entityName);

	public Serializable getKey();

	public boolean isDirty();

	public Object getOwner();

	public boolean isSnapshotEmpty(Serializable snapshot);

	public void postAction();

	public String getRole();

	public void setOwner(Object entity);

	public void forceInitialization();

	public boolean setCurrentSession(SessionImplementor session);

	public Object getValue();

	public boolean unsetSession(SessionImplementor session);

	public void beforeInitialize(CollectionPersister persister, int anticipatedSize);

	public void beginRead();

	public Object readFrom(ResultSet rs, CollectionPersister persister,CollectionAliases descriptor, Object owner) throws ZormException,SQLException;

	public boolean endRead();

	public Iterator entries(CollectionPersister persister);

	public void preInsert(CollectionPersister persister);

	public boolean entryExists(Object entry, int i);

	public Object getIdentifier(Object entry, int i);

	public Object getElement(Object entry);

	public void afterRowInsert(CollectionPersister persister, Object entry, int i);
}
