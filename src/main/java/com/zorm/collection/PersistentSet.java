package com.zorm.collection;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.zorm.exception.ZormException;
import com.zorm.loader.CollectionAliases;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionImplementor;

public class PersistentSet extends AbstractPersistentCollection implements java.util.Set{
	protected Set set;
	protected transient List tempList;
	
	public PersistentSet() {
	}
	
	public PersistentSet(SessionImplementor session) {
		super( session );
	}
	
	public PersistentSet(SessionImplementor session, java.util.Set set) {
		super(session);
		this.set = set;
		setInitialized();
		setDirectlyAccessible(true);
	}

	@Override
	public Iterator entries(CollectionPersister persister) {
		return set.iterator();
	}
	
	public boolean entryExists(Object key, int i) {
		return true;
	}
	
	@Override
	public Object getElement(Object entry) {
		return entry;
	}
	
	@Override
	public Serializable getSnapshot(CollectionPersister persister) {
		return null;
	}

	@Override
	public boolean isSnapshotEmpty(Serializable snapshot) {
		return false;
	}

	@Override
	public void beforeInitialize(CollectionPersister persister,
			int anticipatedSize) {
		
	}

	@Override
	public Object readFrom(ResultSet rs, CollectionPersister persister,
			CollectionAliases descriptor, Object owner) throws ZormException,
			SQLException {
		return null;
	}

	@Override
	public boolean add(Object e) {
		return false;
	}

	@Override
	public boolean addAll(Collection c) {
		return false;
	}

	@Override
	public void clear() {
		
	}

	@Override
	public boolean contains(Object o) {
		return false;
	}

	@Override
	public boolean containsAll(Collection c) {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return readSize() ? getCachedSize()==0 : set.isEmpty();
	}

	@Override
	public Iterator iterator() {
		read();
		return new IteratorProxy(set.iterator());
	}

	@Override
	public boolean remove(Object o) {
		return false;
	}

	@Override
	public boolean removeAll(Collection c) {
		return false;
	}

	@Override
	public boolean retainAll(Collection c) {
		return false;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public Object[] toArray() {
		return null;
	}

	@Override
	public Object[] toArray(Object[] a) {
		return null;
	}

	@Override
	public boolean empty() {
		return false;
	}

	@Override
	public Collection getOrphans(Serializable snapshot, String entityName)
			throws ZormException {
		return null;
	}
}
