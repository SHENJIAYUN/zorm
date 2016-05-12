package com.zorm.engine;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.zorm.collection.AbstractPersistentCollection;
import com.zorm.collection.PersistentCollection;
import com.zorm.exception.AssertionFailure;
import com.zorm.exception.ZormException;
import com.zorm.persister.entity.CollectionPersister;
import com.zorm.session.SessionFactoryImplementor;
import com.zorm.session.SessionImplementor;
import com.zorm.util.MessageHelper;

public final class CollectionEntry implements Serializable {

	private Serializable snapshot;
	private String role;
	private transient CollectionPersister loadedPersister;
	private Serializable loadedKey;
	private transient CollectionPersister currentPersister;
	private transient Serializable currentKey;
	private transient boolean reached;
	private transient boolean processed;
	private transient boolean doupdate;
	private transient boolean doremove;
	private transient boolean dorecreate;
	private transient boolean ignore;
	
	public CollectionEntry(CollectionPersister persister, PersistentCollection collection) {
		ignore = false;

		collection.clearDirty(); //a newly wrapped collection is NOT dirty (or we get unnecessary version updates)

		snapshot = persister.isMutable() ?
				collection.getSnapshot(persister) :
				null;
		collection.setSnapshot(loadedKey, role, snapshot);
	}

	public CollectionEntry(
			final PersistentCollection collection,
			final CollectionPersister loadedPersister,
			final Serializable loadedKey,
			final boolean ignore
	) {
		this.ignore=ignore;
		this.loadedKey = loadedKey;
		setLoadedPersister(loadedPersister);
		collection.setSnapshot(loadedKey, role, null);

	}
	
	@SuppressWarnings("rawtypes")
	public Collection getOrphans(String entityName, PersistentCollection collection){
		if (snapshot==null) {
			throw new AssertionFailure("no collection snapshot for orphan delete");
		}
		return collection.getOrphans( snapshot, entityName );
	}
	
	
	public void setReached(boolean reached) {
		this.reached = reached;
	}

	public void setDoupdate(boolean doupdate) {
		this.doupdate = doupdate;
	}

	public void setDoremove(boolean doremove) {
		this.doremove = doremove;
	}

	public void setDorecreate(boolean dorecreate) {
		this.dorecreate = dorecreate;
	}
	
	public void setProcessed(boolean processed) {
		this.processed = processed;
	}
	
	public boolean isProcessed() {
		return processed;
	}

	public boolean isReached() {
		return reached;
	}
	
	public boolean isIgnore() {
		return ignore;
	}
	
	public boolean isDorecreate() {
		return dorecreate;
	}
	

	public boolean isDoremove() {
		return doremove;
	}
	
	public boolean isDoupdate() {
		return doupdate;
	}
	
	public Serializable getCurrentKey() {
		return currentKey;
	}
	
	public Serializable getSnapshot() {
		return snapshot;
	}
	
	public CollectionPersister getLoadedPersister() {
		return loadedPersister;
	}
	
	public Serializable getLoadedKey() {
		return loadedKey;
	}

	public void preFlush(PersistentCollection collection) throws ZormException {
		if(loadedKey == null && collection.getKey() != null){
			loadedKey = collection.getKey();
		}
		
		boolean nonMutableChange = collection.isDirty() &&
				getLoadedPersister() != null &&
				!getLoadedPersister().isMutable();
		if(nonMutableChange){
			throw new ZormException(
					"changed an immutable collection instance: " +
					MessageHelper.collectionInfoString( getLoadedPersister().getRole(), getLoadedKey() )
				);
		}
		
		setDoupdate(false);
		setDoremove(false);
		setDorecreate(false);
		setReached(false);
		setProcessed(false);
	}

	public CollectionPersister getCurrentPersister() {
		return currentPersister;
	}

	public boolean isSnapshotEmpty(PersistentCollection collection) {
		return collection.wasInitialized() &&
			( getLoadedPersister()==null || getLoadedPersister().isMutable() ) &&
			collection.isSnapshotEmpty( getSnapshot() );
	}
	
	public void setRole(String role) {
		this.role = role;
	}
	
	private void setLoadedPersister(CollectionPersister persister) {
		loadedPersister = persister;
		setRole( persister == null ? null : persister.getRole() );
	}

	public void afterAction(PersistentCollection collection) {
		loadedKey = getCurrentKey();
		setLoadedPersister(getCurrentPersister());
		boolean resnapshot = collection.wasInitialized() &&
				( isDoremove() || isDorecreate() || isDoupdate() );
		if ( resnapshot ) {
			snapshot = loadedPersister==null || !loadedPersister.isMutable() ?
					null :
					collection.getSnapshot(loadedPersister); 
		}
		collection.postAction();
	}

	public void postFlush(PersistentCollection collection) {
		if(isIgnore()){
			ignore = false;
		}
		else if(!isProcessed()){
			throw new AssertionFailure("collection ["+ collection.getRole() + "] was not processed by flush()");
		}
		collection.setSnapshot(loadedKey, role, snapshot);
	}

	public void setCurrentPersister(CollectionPersister persister) {
		this.currentPersister = persister;
	}

	public void setCurrentKey(Serializable currentKey) {
		this.currentKey = currentKey;
	}

	void afterDeserialize(SessionFactoryImplementor factory) {
		loadedPersister = ( factory == null ? null : factory.getCollectionPersister(role) );
	}
	
	private CollectionEntry(
			String role,
	        Serializable snapshot,
	        Serializable loadedKey,
	        SessionFactoryImplementor factory) {
		this.role = role;
		this.snapshot = snapshot;
		this.loadedKey = loadedKey;
		if ( role != null ) {
			afterDeserialize( factory );
		}
	}
	
	public static CollectionEntry deserialize(
			ObjectInputStream ois,
	        SessionImplementor session) throws IOException, ClassNotFoundException {
		return new CollectionEntry(
				( String ) ois.readObject(),
		        ( Serializable ) ois.readObject(),
		        ( Serializable ) ois.readObject(),
		        ( session == null ? null : session.getFactory() )
		);
	}

	public void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeObject( role );
		oos.writeObject( snapshot );
		oos.writeObject( loadedKey );
	}

	public void postInitialize(PersistentCollection collection) throws ZormException {
		snapshot = getLoadedPersister().isMutable() ?
				collection.getSnapshot( getLoadedPersister() ) :
				null;
		collection.setSnapshot(loadedKey, role, snapshot);
		if ( getLoadedPersister().getBatchSize() > 1 ) {
			( (AbstractPersistentCollection) collection ).getSession()
					.getPersistenceContext()
					.getBatchFetchQueue()
					.removeBatchLoadableCollection( this );
		}
	}


}
